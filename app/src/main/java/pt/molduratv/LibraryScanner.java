package pt.molduratv;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import java.util.*;

/** Iterative, read-only tree scan, off the UI thread. */
public final class LibraryScanner {
    public static final class Result {
        public final List<MediaEntry> entries = new ArrayList<>();
        public final List<String> warnings = new ArrayList<>();
    }
    public static Result scan(ContentResolver resolver, List<String> roots) {
        Result result = new Result();
        Set<String> visited = new HashSet<>();
        Set<String> localVisited = new HashSet<>();
        for (String root : roots) {
            if (FolderSource.isLocal(root)) {
                try { LocalFolders.scan(FolderSource.localFile(root), localVisited, result.entries, result.warnings); }
                catch (java.io.IOException | RuntimeException error) { result.warnings.add("Pasta local indisponível. Verifica o disco e as permissões de ficheiros."); }
                continue;
            }
            try { FolderSource.validate(root, false); }
            catch (IllegalArgumentException error) { result.warnings.add("Foi ignorada uma referência de pasta inválida."); continue; }
            Uri tree = Uri.parse(root);
            ArrayDeque<String> pending = new ArrayDeque<>();
            pending.add(DocumentsContract.getTreeDocumentId(tree));
            while (!pending.isEmpty() && !Thread.currentThread().isInterrupted()) {
                String id = pending.removeFirst();
                if (!visited.add(tree.getAuthority() + ":" + id)) continue;
                Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, id);
                String[] columns = {"document_id", "_display_name", "mime_type", "last_modified", "_size"};
                try (Cursor cursor = resolver.query(children, columns, null, null, null)) {
                    if (cursor == null) { result.warnings.add("Uma pasta não respondeu."); continue; }
                    while (cursor.moveToNext()) {
                        if (Thread.currentThread().isInterrupted()) return result;
                        String child = cursor.getString(0), name = cursor.getString(1), mime = cursor.getString(2);
                        if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)) pending.add(child);
                        else {
                            String supported = MediaEntry.supportedMime(name == null ? "" : name, mime);
                            if (supported != null) result.entries.add(new MediaEntry(
                                DocumentsContract.buildDocumentUriUsingTree(tree, child).toString(),
                                name == null ? "Sem nome" : name, supported, cursor.getLong(3), cursor.isNull(4) ? -1 : cursor.getLong(4)));
                        }
                    }
                } catch (RuntimeException error) {
                    result.warnings.add("Uma pasta ficou inacessível. Verifica a ligação ou volta a adicioná-la.");
                }
            }
        }
        return result;
    }
}
