package pt.molduratv;

import android.app.AlertDialog;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;
import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** User-initiated updates. Android always owns installation consent. */
public final class UpdateController {
    private final ComponentActivity activity;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final GitHubUpdates network = new GitHubUpdates();
    private final ActivityResultLauncher<Intent> permission;
    private Future<?> pending;
    private AtomicBoolean cancelled = new AtomicBoolean();
    private AlertDialog progress;
    private int generation;
    private String pendingVersion;
    private boolean busy, closed;

    public UpdateController(ComponentActivity activity, Bundle state) {
        this.activity = activity;
        if (state != null) pendingVersion = state.getString("update.pendingVersion");
        permission = activity.registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (pendingVersion == null) return;
            if (activity.getPackageManager().canRequestPackageInstalls()) validateAndInstall(pendingVersion);
            else message("A instalação não foi autorizada. Podes procurar a atualização novamente quando quiseres.");
        });
    }
    public void saveState(Bundle state) { state.putString("update.pendingVersion", pendingVersion); }
    private File directory() { return new File(activity.getCacheDir(), "updates"); }
    private File downloaded() { return new File(directory(), "update.apk"); }
    private void message(String text) {
        if (!closed && !activity.isDestroyed()) new AlertDialog.Builder(activity)
            .setMessage(text).setPositiveButton("OK", null).show();
    }
    private int begin(String title, String text) {
        busy = true; cancelled = new AtomicBoolean(); int token = ++generation;
        progress = new AlertDialog.Builder(activity).setTitle(title).setMessage(text)
            .setNegativeButton("Cancelar", (dialog, which) -> cancelOperation()).create();
        progress.setOnCancelListener(dialog -> cancelOperation()); progress.show();
        return token;
    }
    private void finish() {
        busy = false;
        if (progress != null) { progress.dismiss(); progress = null; }
    }
    private void post(int token, Runnable action) {
        main.post(() -> { if (!closed && !activity.isDestroyed() && token == generation) action.run(); });
    }
    private void failed(int token, Exception error) {
        if (error instanceof CancellationException) return;
        String notice;
        if (error instanceof org.json.JSONException) notice = "A informação da Release está incompleta ou inválida. Verifica a publicação no GitHub.";
        else if (error instanceof java.net.UnknownHostException || error instanceof java.net.SocketTimeoutException)
            notice = "Não foi possível ligar ao GitHub. Verifica a ligação à Internet e tenta novamente.";
        else notice = error.getMessage() == null ? "Não foi possível verificar a atualização. Tenta novamente." : error.getMessage();
        post(token, () -> { finish(); message(notice); });
    }
    public void check() {
        if (closed || busy) return;
        String installed;
        try { installed = UpdatePackageVerifier.installed(activity).versionName; }
        catch (Exception error) { message("Não foi possível identificar a versão instalada."); return; }
        int token = begin("Atualizações", "A procurar uma nova versão no GitHub…\nVersão instalada: " + installed);
        AtomicBoolean stop = cancelled;
        pending = worker.submit(() -> {
            try {
                ReleaseUpdate release = network.check(installed, stop);
                post(token, () -> {
                    finish();
                    if (release == null) { message("Já tens a versão mais recente publicada no GitHub (" + installed + ")."); return; }
                    new AlertDialog.Builder(activity).setTitle("Nova versão: " + release.version)
                        .setMessage("Versão instalada: " + installed + "\nTamanho: " + String.format(java.util.Locale.getDefault(), "%.1f MB", release.size / 1_000_000d)
                            + "\n\nQueres descarregar a atualização e abrir o instalador do Android?")
                        .setPositiveButton("Descarregar", (dialog, which) -> download(release))
                        .setNeutralButton("Ver no GitHub", (dialog, which) -> openReleases())
                        .setNegativeButton("Agora não", null).show();
                });
            } catch (Exception error) { failed(token, error); }
        });
    }
    private void download(ReleaseUpdate release) {
        if (closed || busy) return;
        int token = begin("Descarregar atualização", "A descarregar a versão " + release.version + "…");
        AtomicBoolean stop = cancelled;
        pending = worker.submit(() -> {
            try {
                long[] lastUpdate = {0};
                File file = network.download(release, directory(), stop, bytes -> {
                    long now = System.nanoTime();
                    if (now - lastUpdate[0] > 500_000_000L) {
                        lastUpdate[0] = now;
                        post(token, () -> { if (progress != null) progress.setMessage("A descarregar a versão " + release.version
                            + "… " + bytes * 100 / release.size + "%"); });
                    }
                });
                UpdatePackageVerifier.verify(activity, file, release.version);
                post(token, () -> { finish(); pendingVersion = release.version; requestInstall(); });
            } catch (Exception error) {
                // The sandboxed cache entry is never offered to the installer on failure.
                failed(token, error);
            }
        });
    }
    private void requestInstall() {
        if (!activity.getPackageManager().canRequestPackageInstalls()) {
            new AlertDialog.Builder(activity).setTitle("Autorizar a atualização")
                .setMessage("O Android precisa que autorizes a MolduraTV a instalar atualizações. Depois de autorizares, volta à aplicação para continuar.")
                .setPositiveButton("Abrir definições", (dialog, which) -> {
                    try { permission.launch(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:" + activity.getPackageName()))); }
                    catch (ActivityNotFoundException error) { message("Esta box não disponibiliza essa página. Verifica as autorizações de instalação nas definições do Android."); }
                }).setNegativeButton("Cancelar", null).show();
        } else validateAndInstall(pendingVersion);
    }
    private void validateAndInstall(String version) {
        if (closed || busy || version == null) return;
        int token = begin("Verificar atualização", "A confirmar a versão e a assinatura antes de abrir o instalador…");
        pending = worker.submit(() -> {
            try {
                UpdatePackageVerifier.verify(activity, downloaded(), version);
                post(token, () -> {
                    finish();
                    try {
                        Uri uri = FileProvider.getUriForFile(activity, activity.getPackageName() + ".updates", downloaded());
                        Intent install = new Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        install.setClipData(ClipData.newRawUri("MolduraTV", uri));
                        activity.startActivity(install);
                    } catch (ActivityNotFoundException | SecurityException error) {
                        message("Não foi possível abrir o instalador nesta box. Podes obter a versão na página de Releases do GitHub.");
                    }
                });
            } catch (Exception error) { failed(token, error); }
        });
    }
    private void openReleases() {
        try { activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(ReleaseUpdate.RELEASES_URL))); }
        catch (ActivityNotFoundException error) { message("Esta box não tem um navegador disponível. Abre github.com/renafcampos27/Moldura_Digital/releases noutro equipamento."); }
    }
    private void cancelOperation() {
        cancelled.set(true); generation++; network.cancel();
        if (pending != null) pending.cancel(true);
        finish();
    }
    public void close() {
        closed = true; cancelOperation(); worker.shutdownNow(); main.removeCallbacksAndMessages(null);
    }
}
