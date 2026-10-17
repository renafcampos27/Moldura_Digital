package pt.molduratv;

import android.Manifest;
import android.app.AlertDialog;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.widget.*;
import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/** Built-in, read-only navigation. Only the explicit backup action writes files. */
public final class StorageController {
    public enum Mode { FOLDER, OPEN_BACKUP, SAVE_BACKUP }
    private final ComponentActivity activity;
    private final Runnable changed;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ActivityResultLauncher<String[]> legacyPermission;
    private final ActivityResultLauncher<Intent> specialPermission;
    private Runnable afterPermission;
    private AlertDialog visible;
    private boolean closed;
    private int generation;

    public StorageController(ComponentActivity activity, Runnable changed) {
        this.activity = activity; this.changed = changed;
        legacyPermission = activity.registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> permissionResult());
        specialPermission = activity.registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> permissionResult());
    }
    @SuppressWarnings("deprecation") // Legacy permissions apply only to Android 8–10.
    public boolean sharedReadable() {
        return Build.VERSION.SDK_INT >= 30 ? Environment.isExternalStorageManager()
            : ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }
    @SuppressWarnings("deprecation")
    private boolean sharedWritable() {
        return Build.VERSION.SDK_INT >= 30 ? Environment.isExternalStorageManager()
            : ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }
    public void showPermissions() {
        if (closed) return;
        AlertDialog dialog = new AlertDialog.Builder(activity).setTitle("Permissões de ficheiros")
            .setMessage("Pastas da MolduraTV: disponíveis\nLeitura de armazenamento partilhado: " + (sharedReadable() ? "autorizada" : "por autorizar")
                + "\nEscrita de armazenamento partilhado: " + (sharedWritable() ? "autorizada" : "por autorizar")
                + "\n\nAutoriza o acesso para escolher pastas no armazenamento interno e nos discos montados. A escrita é usada ao guardar backups."
                + "\n\nO Android mantém protegidas as pastas privadas de outras aplicações. Um disco só de leitura continua sem permitir escrita.")
            .setPositiveButton("Autorizar / gerir", (unused, which) -> requestAccess())
            .setNeutralButton("Definições da app", (unused, which) -> {
                try { specialPermission.launch(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + activity.getPackageName()))); }
                catch (ActivityNotFoundException | SecurityException error) { afterPermission = null; message("Abre manualmente as definições da MolduraTV no Android."); }
            })
            .setNegativeButton("Fechar", (unused, which) -> afterPermission = null).create();
        dialog.setOnCancelListener(unused -> afterPermission = null); dialog.show();
    }
    private void permissionResult() {
        if (closed) return;
        changed.run(); Runnable resume = afterPermission; afterPermission = null;
        if (sharedReadable() && resume != null) resume.run();
        else if (!sharedReadable()) message("O acesso geral a ficheiros ainda não está autorizado. As pastas da própria MolduraTV continuam disponíveis. Podes voltar a tentar em Permissões.");
    }
    @SuppressWarnings("deprecation")
    private void requestAccess() {
        if (Build.VERSION.SDK_INT < 30) {
            legacyPermission.launch(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE});
            return;
        }
        Intent[] options = {
            new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:" + activity.getPackageName())),
            new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION),
            new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + activity.getPackageName()))
        };
        for (Intent intent : options) {
            try { specialPermission.launch(intent); return; }
            catch (ActivityNotFoundException | SecurityException ignored) { }
        }
        afterPermission = null;
        message("Esta box não disponibiliza a página de autorização. Procura nas definições do Android: Aplicações → Acesso especial → Acesso a todos os ficheiros → MolduraTV.");
    }
    public void choose(Mode mode, String name, File initial, Consumer<File> selected) {
        if (initial != null && initial.isDirectory()) showDirectory(mode, name, initial, initial, selected);
        else showLocations(mode, name, selected);
    }
    private <T> void load(Callable<T> work, Consumer<T> done, Runnable retry) {
        if (closed) return;
        int token = ++generation;
        if (visible != null) visible.dismiss();
        visible = new AlertDialog.Builder(activity).setMessage("A ler as pastas…")
            .setNegativeButton("Cancelar", (dialog, which) -> generation++).create();
        visible.setOnCancelListener(dialog -> generation++); visible.show();
        io.execute(() -> {
            try {
                T value = work.call();
                main.post(() -> { if (!closed && token == generation && !activity.isDestroyed()) { visible.dismiss(); done.accept(value); } });
            } catch (Exception error) {
                main.post(() -> { if (!closed && token == generation && !activity.isDestroyed()) {
                    visible.dismiss();
                    new AlertDialog.Builder(activity).setTitle("Pasta indisponível")
                        .setMessage("Não foi possível ler esta pasta. Verifica se o disco está ligado e se a MolduraTV tem autorização de ficheiros.")
                        .setPositiveButton("Permissões", (dialog, which) -> { afterPermission = retry; showPermissions(); })
                        .setNegativeButton("Fechar", null).show();
                } });
            }
        });
    }
    private void showLocations(Mode mode, String name, Consumer<File> selected) {
        load(() -> StorageLocations.discover(activity.getApplicationContext()), locations -> {
            List<String> labels = new ArrayList<>(); List<Runnable> actions = new ArrayList<>();
            labels.add("Permissões de ficheiros"); actions.add(this::showPermissions);
            for (StorageLocations.Location location : locations) {
                labels.add(location.label + "\n" + location.directory.getPath());
                actions.add(() -> {
                    Runnable browse = () -> showDirectory(mode, name, location.directory, location.directory, selected);
                    if (location.appOwned || sharedReadable()) browse.run();
                    else new AlertDialog.Builder(activity).setTitle("Autorizar acesso aos ficheiros")
                        .setMessage("Para abrir este armazenamento, autoriza o acesso da MolduraTV aos ficheiros. Depois de regressares, a navegação continua aqui.")
                        .setPositiveButton("Autorizar", (dialog, which) -> { afterPermission = browse; requestAccess(); })
                        .setNegativeButton("Cancelar", null).show();
                });
            }
            labels.add("Introduzir caminho de uma pasta"); actions.add(() -> manualPath(mode, name, selected));
            labels.add("Atualizar discos e locais"); actions.add(() -> showLocations(mode, name, selected));
            visible = new AlertDialog.Builder(activity).setTitle("Escolher armazenamento")
                .setItems(labels.toArray(new String[0]), (dialog, which) -> actions.get(which).run())
                .setNegativeButton("Cancelar", null).show();
        }, () -> showLocations(mode, name, selected));
    }
    private void manualPath(Mode mode, String name, Consumer<File> selected) {
        EditText input = new EditText(activity); input.setSingleLine(true); input.setText(R.string.storage_initial_path);
        AlertDialog dialog = new AlertDialog.Builder(activity).setTitle("Caminho absoluto da pasta").setView(input)
            .setPositiveButton("Abrir", null).setNegativeButton("Cancelar", null).create();
        dialog.setOnShowListener(unused -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            File directory = new File(input.getText().toString().trim());
            if (!directory.isAbsolute()) { input.setError("Indica um caminho absoluto, começado por /."); return; }
            dialog.dismiss(); showDirectory(mode, name, directory, directory, selected);
        })); dialog.show();
    }
    private void showDirectory(Mode mode, String name, File boundary, File directory, Consumer<File> selected) {
        load(() -> LocalFolders.children(directory, mode == Mode.OPEN_BACKUP), children -> {
            List<String> labels = new ArrayList<>(); List<Runnable> actions = new ArrayList<>();
            if (mode != Mode.OPEN_BACKUP) {
                labels.add(mode == Mode.FOLDER ? "✓ Usar esta pasta" : "✓ Guardar backup nesta pasta");
                actions.add(() -> {
                    if (mode == Mode.SAVE_BACKUP && (name == null || name.contains("/") || name.contains("\\") || name.equals(".") || name.equals(".."))) {
                        message("Nome de backup inválido."); return;
                    }
                    selected.accept(mode == Mode.FOLDER ? directory : new File(directory, name));
                });
            }
            labels.add("⌂ Voltar aos armazenamentos"); actions.add(() -> showLocations(mode, name, selected));
            File parent = directory.getParentFile();
            if (parent != null && !directory.equals(boundary) && parent.toPath().normalize().startsWith(boundary.toPath().normalize())) {
                labels.add("↑ Pasta acima"); actions.add(() -> showDirectory(mode, name, boundary, parent, selected));
            }
            for (File child : children) {
                labels.add((child.isDirectory() ? "▸ " : "Backup · ") + child.getName());
                actions.add(() -> {
                    if (child.isDirectory()) showDirectory(mode, name, boundary, child, selected);
                    else selected.accept(child);
                });
            }
            LinearLayout layout = new LinearLayout(activity); layout.setOrientation(LinearLayout.VERTICAL);
            int padding = Math.round(20 * activity.getResources().getDisplayMetrics().density);
            layout.setPadding(padding, 0, padding, 0);
            TextView path = new TextView(activity); path.setText(mode == Mode.SAVE_BACKUP
                ? activity.getString(R.string.local_backup_path_hint, directory.getPath()) : directory.getPath()); path.setTextSize(15);
            path.setMaxLines(mode == Mode.SAVE_BACKUP ? 3 : 2); layout.addView(path);
            ListView list = new ListView(activity); list.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1, labels));
            layout.addView(list, new LinearLayout.LayoutParams(-1, Math.round(240 * activity.getResources().getDisplayMetrics().density)));
            visible = new AlertDialog.Builder(activity).setTitle(mode == Mode.FOLDER ? "Escolher pasta" : mode == Mode.SAVE_BACKUP ? "Guardar backup" : "Abrir backup")
                .setView(layout).setNegativeButton("Cancelar", null)
                .setNeutralButton("Permissões", (dialog, which) -> { afterPermission = () -> showDirectory(mode, name, boundary, directory, selected); showPermissions(); }).create();
            AlertDialog browser = visible;
            list.setOnItemClickListener((adapter, view, index, id) -> { browser.dismiss(); actions.get(index).run(); });
            visible.show(); list.requestFocus(); list.setSelection(0);
        }, () -> showDirectory(mode, name, boundary, directory, selected));
    }
    private void message(String message) {
        if (!closed && !activity.isDestroyed()) new AlertDialog.Builder(activity).setMessage(message).setPositiveButton("OK", null).show();
    }
    public void close() {
        closed = true; generation++; io.shutdownNow(); main.removeCallbacksAndMessages(null);
        if (visible != null) visible.dismiss(); afterPermission = null;
    }
}
