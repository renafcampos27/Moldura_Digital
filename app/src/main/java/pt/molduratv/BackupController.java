package pt.molduratv;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.provider.DocumentsContract;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executor;
import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

/** SAF file operations; parsing and IO never run on the UI thread. */
public final class BackupController {
    private static final int EXPORT = 40, IMPORT = 41, REAUTHORIZE = 42;
    public interface Host {
        boolean canEdit();
        void busy(boolean busy);
        void changed();
    }
    private final ComponentActivity activity;
    private final ActivityResultLauncher<Intent> exportPicker, importPicker, folderPicker;
    private final AppSettings settings;
    private final Executor executor;
    private final Host host;
    private final StorageController storage;
    private final Handler main = new Handler(Looper.getMainLooper());
    private String reconnectRoot;

    public BackupController(ComponentActivity activity, AppSettings settings, Executor executor, Host host, StorageController storage, Bundle state) {
        this.activity = activity; this.settings = settings; this.executor = executor; this.host = host;
        this.storage = storage;
        exportPicker = activity.registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> onResult(EXPORT, result.getResultCode(), result.getData()));
        importPicker = activity.registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> onResult(IMPORT, result.getResultCode(), result.getData()));
        folderPicker = activity.registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> onResult(REAUTHORIZE, result.getResultCode(), result.getData()));
        if (state != null) reconnectRoot = state.getString("backup.reconnectRoot");
    }
    public void saveState(Bundle state) { state.putString("backup.reconnectRoot", reconnectRoot); }
    private void message(String value) {
        if (!activity.isDestroyed()) new AlertDialog.Builder(activity).setMessage(value).setPositiveButton("OK", null).show();
    }
    public void showMenu() {
        if (!host.canEdit()) { message("Aguarda que a operação atual termine."); return; }
        new AlertDialog.Builder(activity).setTitle("Backups da coleção")
            .setItems(new String[]{"Guardar backup", "Restaurar backup", "Rever acesso às pastas", "Outros locais do Android / rede"}, (dialog, which) -> {
                if (which == 0) {
                    String name = "MolduraTV-backup-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".json";
                    storage.choose(StorageController.Mode.SAVE_BACKUP, name, null, file -> localResult(EXPORT, file));
                } else if (which == 1) {
                    storage.choose(StorageController.Mode.OPEN_BACKUP, null, null, file -> localResult(IMPORT, file));
                } else if (which == 2) reviewAccess();
                else new AlertDialog.Builder(activity).setTitle("Seletor de documentos do Android")
                    .setItems(new String[]{"Guardar backup", "Restaurar backup"}, (other, selected) -> {
                        if (selected == 0) launch(new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                            .setType("application/json").putExtra(Intent.EXTRA_TITLE, "MolduraTV-backup-"
                                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".json"), EXPORT);
                        else launch(new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*"), IMPORT);
                    }).setNegativeButton("Fechar", null).show();
            }).setNegativeButton("Fechar", null).show();
    }
    private void localResult(int request, File file) {
        try { onResult(request, Activity.RESULT_OK, new Intent().setData(Uri.parse(FolderSource.fromFile(file)))); }
        catch (IOException error) { message("Não foi possível abrir esta localização."); }
    }
    private void launch(Intent intent, int request) {
        try { (request == EXPORT ? exportPicker : request == IMPORT ? importPicker : folderPicker).launch(intent); }
        catch (ActivityNotFoundException error) { message("O seletor do Android não está disponível. Usa Guardar backup ou Restaurar backup no menu anterior para navegar diretamente nas pastas e discos."); }
    }
    public boolean onResult(int request, int result, Intent intent) {
        if (request != EXPORT && request != IMPORT && request != REAUTHORIZE) return false;
        if (result != Activity.RESULT_OK || intent == null || intent.getData() == null) {
            if (request == REAUTHORIZE) reconnectRoot = null;
            return true;
        }
        Uri uri = intent.getData();
        if (request == REAUTHORIZE) { reconnect(uri); return true; }
        if (!host.canEdit()) { message("Aguarda que a operação atual termine e tenta novamente."); return true; }
        if (request == EXPORT) exportTo(uri); else importFrom(uri);
        return true;
    }
    private void exportTo(Uri uri) {
        final SettingsBackup.Data snapshot;
        try { snapshot = settings.snapshot(); }
        catch (IllegalArgumentException error) { message("Não foi possível preparar o backup. Verifica as pastas e as opções."); return; }
        host.busy(true);
        ContentResolver resolver = activity.getApplicationContext().getContentResolver();
        executor.execute(() -> {
            String result;
            try {
                if ("file".equals(uri.getScheme())) SettingsBackup.writeNew(FolderSource.localFile(uri.toString()), snapshot);
                else try (OutputStream output = resolver.openOutputStream(uri, "wt")) {
                    if (output == null) throw new IOException("Destino indisponível.");
                    output.write(SettingsBackup.encode(snapshot).getBytes(StandardCharsets.UTF_8)); output.flush();
                }
                result = "Backup guardado com a seleção de pastas e as opções da apresentação.";
            } catch (Exception error) { result = "Não foi possível guardar o backup. Verifica o destino e o espaço disponível; um ficheiro incompleto pode ter ficado no destino."; }
            String notice = result;
            main.post(() -> { if (!activity.isDestroyed()) { host.busy(false); message(notice); } });
        });
    }
    private void importFrom(Uri uri) {
        host.busy(true);
        ContentResolver resolver = activity.getApplicationContext().getContentResolver();
        executor.execute(() -> {
            try (InputStream input = "file".equals(uri.getScheme())
                    ? new FileInputStream(FolderSource.localFile(uri.toString())) : resolver.openInputStream(uri)) {
                SettingsBackup.Data data = SettingsBackup.read(input);
                main.post(() -> {
                    if (activity.isDestroyed()) return;
                    host.busy(false); preview(data);
                });
            } catch (Exception error) {
                main.post(() -> { if (!activity.isDestroyed()) {
                    host.busy(false); message("Não foi possível ler um backup válido da MolduraTV. A configuração atual foi preservada.");
                } });
            }
        });
    }
    private void preview(SettingsBackup.Data data) {
        String order = new String[]{"Nome", "Data de modificação", "Aleatória","Data de captura"}[data.order];
        new AlertDialog.Builder(activity).setTitle("Restaurar backup?")
            .setMessage(data.roots.size() + " pasta(s)\n" + data.seconds + " segundos por fotografia\nOrdem: " + order
                + "\nImagem: " + (data.crop ? "Preencher com corte" : "Mostrar inteira")
                + "\nApresentação: " + PhotoSlides.modeName(data.layoutMode)
                + "\nMotion Photos: " + MotionPhotos.modeName(data.motionMode)
                + "\nLive Photos: " + LivePhotos.modeName(data.liveMode)
                + "\nFiltros: " + data.filter.summary()
                + "\nSom: " + (data.muted ? "Desligado" : "Ligado") + "\nRepetir: " + (data.loop ? "Sim" : "Não")
                + "\n\nA seleção de pastas e as opções atuais serão substituídas. Poderá ser necessário voltar a autorizar as pastas.")
            .setPositiveButton("Restaurar", (dialog, which) -> {
                if (!host.canEdit()) { message("Aguarda que a operação atual termine e tenta novamente."); return; }
                settings.restore(data); host.changed();
                message("Backup restaurado. Se alguma pasta estiver sem acesso, abre Backups → Rever acesso às pastas para a voltar a autorizar.");
            }).setNegativeButton("Cancelar", null).show();
    }
    private boolean authorized(String root) {
        if (FolderSource.isLocal(root)) {
            try { File folder = FolderSource.localFile(root); return folder.isDirectory() && folder.canRead(); }
            catch (IOException error) { return false; }
        }
        for (UriPermission grant : activity.getContentResolver().getPersistedUriPermissions())
            if (grant.isReadPermission() && grant.getUri().toString().equals(root)) return true;
        return false;
    }
    private String folderName(String root) { return FolderSource.display(root); }
    public void reviewAccess() {
        if (!host.canEdit()) { message("Aguarda que a operação atual termine."); return; }
        if (settings.roots.isEmpty()) { message("Ainda não existem pastas na coleção."); return; }
        String[] labels = settings.roots.stream().map(root -> (authorized(root) ? "Autorizada · " : "Autorizar · ") + folderName(root)).toArray(String[]::new);
        new AlertDialog.Builder(activity).setTitle("Escolhe a pasta para voltar a autorizar")
            .setItems(labels, (dialog, which) -> {
                reconnectRoot = settings.roots.get(which);
                if (FolderSource.isLocal(reconnectRoot)) {
                    File initial;
                    try { initial = FolderSource.localFile(reconnectRoot); }
                    catch (IOException error) { initial = null; }
                    storage.choose(StorageController.Mode.FOLDER, null, initial, file -> localResult(REAUTHORIZE, file));
                    return;
                }
                new AlertDialog.Builder(activity).setTitle("Localizar novamente a pasta")
                    .setItems(new String[]{"Armazenamento e discos da box", "Seletor do Android / rede"}, (method, selected) -> {
                        if (selected == 0) storage.choose(StorageController.Mode.FOLDER, null, null, file -> localResult(REAUTHORIZE, file));
                        else launch(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                            .putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.parse(reconnectRoot)), REAUTHORIZE);
                    }).setNegativeButton("Cancelar", null).show();
            }).setNegativeButton("Fechar", null).show();
    }
    private void reconnect(Uri uri) {
        String original = reconnectRoot; reconnectRoot = null;
        if (original == null || !settings.roots.contains(original)) { message("Volta a escolher a pasta que pretendes autorizar."); return; }
        try {
            new SettingsBackup.Data(java.util.Collections.singletonList(uri.toString()), settings.seconds, settings.order, settings.crop, settings.muted, settings.loop);
            if (!FolderSource.isLocal(uri.toString())) activity.getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            int index = settings.roots.indexOf(original);
            if (!settings.roots.contains(uri.toString())) settings.roots.set(index, uri.toString());
            else if (!original.equals(uri.toString())) settings.roots.remove(index);
            settings.save(); host.changed();
        } catch (RuntimeException error) { message("Não foi possível autorizar a pasta. A seleção anterior foi preservada."); }
    }
}
