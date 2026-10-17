package pt.molduratv;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import org.json.JSONArray;
import java.util.*;

/** App-lifetime SQLite helper. All calls are made on workers; no media bytes are stored. */
final class CollectionIndex extends SQLiteOpenHelper {
    private static CollectionIndex instance;
    static synchronized CollectionIndex get(Context context) {
        if (instance == null) instance = new CollectionIndex(context.getApplicationContext());
        return instance;
    }
    private CollectionIndex(Context context) { super(context,"collection-index.db",null,2); }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE collection(uri TEXT PRIMARY KEY,name TEXT,mime TEXT,modified INTEGER,bytes INTEGER)");
        db.execSQL("CREATE TABLE metadata(uri TEXT PRIMARY KEY,modified INTEGER,bytes INTEGER,ratio REAL,motion INTEGER,captured INTEGER DEFAULT 0)");
        db.execSQL("CREATE TABLE configuration(id INTEGER PRIMARY KEY,roots TEXT)");
    }
    @Override public void onUpgrade(SQLiteDatabase db, int old, int next) {
        if(old==1 && next==2) { db.execSQL("ALTER TABLE metadata ADD COLUMN captured INTEGER DEFAULT 0"); db.delete("metadata",null,null); }
        else throw new IllegalStateException("Unsupported index schema");
    }
    synchronized List<MediaEntry> load(List<String> roots) {
        List<MediaEntry> result = new ArrayList<>();
        try {
            SQLiteDatabase db = getReadableDatabase();
            try (Cursor cursor = db.rawQuery("SELECT roots FROM configuration WHERE id=1",null)) {
                if (!cursor.moveToFirst() || !new JSONArray(roots).toString().equals(cursor.getString(0))) return result;
            }
            try (Cursor cursor = db.rawQuery("SELECT uri,name,mime,modified,bytes FROM collection LIMIT 50000",null)) {
                while (cursor.moveToNext()) {
                    if (Thread.currentThread().isInterrupted()) { result.clear(); return result; }
                    result.add(new MediaEntry(cursor.getString(0),cursor.getString(1),cursor.getString(2),cursor.getLong(3),cursor.getLong(4)));
                }
            }
        } catch (RuntimeException ignored) { result.clear(); }
        return result;
    }
    synchronized boolean save(List<String> roots, List<MediaEntry> entries) {
        SQLiteDatabase db;
        try { db = getWritableDatabase(); db.beginTransaction(); }
        catch (RuntimeException error) { return false; }
        try {
            db.delete("collection",null,null);
            if (entries.size() > 50000) {
                db.delete("configuration",null,null); db.setTransactionSuccessful(); return true;
            }
            for (MediaEntry entry : entries) {
                if (Thread.currentThread().isInterrupted()) return false;
                ContentValues row = new ContentValues(); row.put("uri",entry.uri); row.put("name",entry.name);
                row.put("mime",entry.mime); row.put("modified",entry.modified); row.put("bytes",entry.size);
                if (db.insertWithOnConflict("collection",null,row,SQLiteDatabase.CONFLICT_REPLACE) < 0) throw new SQLiteException("Index insert failed");
            }
            ContentValues config = new ContentValues(); config.put("id",1); config.put("roots",new JSONArray(roots).toString());
            if(db.insertWithOnConflict("configuration",null,config,SQLiteDatabase.CONFLICT_REPLACE)<0) throw new SQLiteException("Index configuration failed");
            db.setTransactionSuccessful(); return true;
        } catch (RuntimeException error) { return false; }
        finally { db.endTransaction(); }
    }
    synchronized PhotoMetadata.Info metadata(MediaEntry entry) {
        if (entry.modified <= 0 || entry.size <= 0) return null;
        try (Cursor cursor = getReadableDatabase().rawQuery("SELECT ratio,motion,captured FROM metadata WHERE uri=? AND modified=? AND bytes=?",
                new String[]{entry.uri,Long.toString(entry.modified),Long.toString(entry.size)})) {
            if (cursor.moveToFirst()) return new PhotoMetadata.Info(cursor.getDouble(0),cursor.getInt(1) != 0,cursor.getLong(2));
        } catch (RuntimeException ignored) { }
        return null;
    }
    synchronized void saveMetadata(Map<MediaEntry,PhotoMetadata.Info> values) {
        SQLiteDatabase db;
        try { db = getWritableDatabase(); db.beginTransaction(); } catch (RuntimeException error) { return; }
        try {
            for (Map.Entry<MediaEntry,PhotoMetadata.Info> value : values.entrySet()) {
                MediaEntry entry = value.getKey(); PhotoMetadata.Info info = value.getValue();
                if (entry.modified <= 0 || entry.size <= 0 || (info.ratio <= 0 && info.captured<=0)) continue;
                ContentValues row = new ContentValues(); row.put("uri",entry.uri); row.put("modified",entry.modified);
                row.put("bytes",entry.size); row.put("ratio",info.ratio); row.put("motion",info.motion ? 1 : 0);
                row.put("captured",info.captured);
                if(db.insertWithOnConflict("metadata",null,row,SQLiteDatabase.CONFLICT_REPLACE)<0) throw new SQLiteException("Index metadata failed");
            }
            db.execSQL("DELETE FROM metadata WHERE rowid NOT IN (SELECT rowid FROM metadata ORDER BY rowid DESC LIMIT 50000)");
            db.setTransactionSuccessful();
        } catch (RuntimeException ignored) { }
        finally { db.endTransaction(); }
    }
    synchronized boolean clearIndex() {
        SQLiteDatabase db;
        try { db=getWritableDatabase(); db.beginTransaction(); } catch (RuntimeException error) { return false; }
        try {
            db.delete("collection",null,null); db.delete("metadata",null,null); db.delete("configuration",null,null);
            db.setTransactionSuccessful(); return true;
        } catch (RuntimeException error) { return false; }
        finally { db.endTransaction(); }
    }
}
