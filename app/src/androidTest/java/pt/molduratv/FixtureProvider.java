package pt.molduratv;

import android.database.*;
import android.os.*;
import android.provider.DocumentsContract.Document;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.net.Uri;
import java.io.*;

public final class FixtureProvider extends ContentProvider {
    private static final String[] COLUMNS = {"document_id", "_display_name", "mime_type", "last_modified", "_size", "flags"};
    @Override public boolean onCreate() { return true; }
    @Override public Cursor query(Uri uri, String[] projection, String selection, String[] args, String order) {
        java.util.List<String> parts = uri.getPathSegments();
        if ("children".equals(uri.getLastPathSegment())) return queryChildDocuments(parts.get(parts.size() - 2), projection, order);
        return queryDocument(uri.getLastPathSegment(), projection);
    }
    @Override public String getType(Uri uri) { return uri.toString().endsWith(".mp4") ? "video/mp4" : "image/jpeg"; }
    @Override public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] args) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri uri, String selection, String[] args) { throw new UnsupportedOperationException(); }
    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException { return openDocument(uri.getLastPathSegment(), mode, null); }
    public Cursor queryDocument(String id, String[] projection) {
        MatrixCursor cursor = new MatrixCursor(COLUMNS); row(cursor, id); return cursor;
    }
    public Cursor queryChildDocuments(String parent, String[] projection, String sortOrder) {
        MatrixCursor cursor = new MatrixCursor(COLUMNS);
        if (parent.equals("root")) for (String id : new String[]{"01.jpg", "02.mp4", "03.jpg"}) row(cursor, id);
        if (parent.equals("broken")) for (String id : new String[]{"00-invalid.jpg", "03.jpg"}) row(cursor, id);
        if (parent.equals("pairs")) for (String id : new String[]{"11-portrait.jpg", "12-portrait.jpg", "13-portrait.jpg", "14.mp4"}) row(cursor, id);
        if (parent.equals("multiplepairs")) for (String id : new String[]{"21-portrait.jpg", "22-portrait.jpg", "23-portrait.jpg", "24-portrait.jpg"}) row(cursor, id);
        if (parent.equals("videopairs")) for (String id : new String[]{"31-short.mp4", "32-long.mp4", "33.jpg"}) row(cursor, id);
        if (parent.equals("videopairsreverse")) for (String id : new String[]{"31-long.mp4", "32-short.mp4", "33.jpg"}) row(cursor, id);
        if (parent.equals("widephotos")) for (String id : new String[]{"41.jpg", "42.jpg"}) row(cursor, id);
        if (parent.equals("motion")) row(cursor,"motion.jpg");
        return cursor;
    }
    private void row(MatrixCursor cursor, String id) {
        cursor.addRow(new Object[]{id, id, id.equals("root") || id.equals("broken") || id.equals("pairs") || id.equals("multiplepairs") || id.startsWith("videopairs") || id.equals("widephotos") || id.equals("motion") ? Document.MIME_TYPE_DIR
            : id.endsWith(".mp4") ? "video/mp4" : "image/jpeg", 1L, 0L, 0});
    }
    public ParcelFileDescriptor openDocument(String id, String mode, CancellationSignal signal) throws FileNotFoundException {
        if (id.equals("backup.json")) {
            File backup = new File(getContext().getCacheDir(), "backup.json");
            return ParcelFileDescriptor.open(backup, mode.contains("w")
                ? ParcelFileDescriptor.MODE_WRITE_ONLY | ParcelFileDescriptor.MODE_CREATE | ParcelFileDescriptor.MODE_TRUNCATE
                : ParcelFileDescriptor.MODE_READ_ONLY);
        }
        if (!mode.equals("r")) throw new FileNotFoundException("Read only");
        File file = new File(getContext().getCacheDir(), id);
        if (id.contains("portrait")) {
            android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(360, 640, android.graphics.Bitmap.Config.ARGB_8888);
            bitmap.eraseColor(id.startsWith("11") ? android.graphics.Color.RED : android.graphics.Color.GREEN);
            try (OutputStream output = new FileOutputStream(file)) {
                if (!bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, output)) throw new IOException("Fixture encode failed");
            } catch (IOException error) { throw new FileNotFoundException(error.getMessage()); }
            finally { bitmap.recycle(); }
            return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        }
        try (InputStream input = id.equals("00-invalid.jpg") ? new ByteArrayInputStream(new byte[]{1,2,3})
                : getContext().getAssets().open(id.equals("motion.jpg") ? "motion.jpg" : id.endsWith("-short.mp4") ? "portrait-short.mp4"
                    : id.endsWith("-long.mp4") ? "portrait-long.mp4" : id.endsWith(".mp4") ? "clip.mp4" : "photo.jpg");
             OutputStream output = new FileOutputStream(file)) {
            byte[] bytes = new byte[8192]; int count;
            while ((count = input.read(bytes)) != -1) output.write(bytes, 0, count);
        } catch (IOException error) { throw new FileNotFoundException(error.getMessage()); }
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
    }
}
