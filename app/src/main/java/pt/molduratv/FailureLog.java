package pt.molduratv;

import org.json.*;
import java.util.*;

/** Bounded, deduplicated diagnostic history; no stack traces, credentials or media bytes. */
public final class FailureLog {
    public static final int LIMIT = 50;
    public static final class Entry {
        public final String file, category, detail;
        public final long time;
        public final int count;
        Entry(String file,String category,String detail,long time,int count) {
            this.file=file; this.category=category; this.detail=detail; this.time=time; this.count=count;
        }
    }
    private final List<Entry> entries = new ArrayList<>();
    public List<Entry> entries() { return Collections.unmodifiableList(new ArrayList<>(entries)); }
    public void clear() { entries.clear(); }
    public void add(String file,String category,String detail,long time) {
        file = bounded(file); category = bounded(category); detail = bounded(detail);
        int count = 1;
        for (Iterator<Entry> iterator = entries.iterator(); iterator.hasNext();) {
            Entry previous = iterator.next();
            if (previous.file.equals(file) && previous.category.equals(category) && previous.detail.equals(detail)) {
                count = Math.min(1000000,previous.count+1); iterator.remove(); break;
            }
        }
        entries.add(0,new Entry(file,category,detail,time,count));
        while (entries.size() > LIMIT) entries.remove(entries.size()-1);
    }
    private static String bounded(String text) { return text == null ? "" : text.substring(0,Math.min(300,text.length())).replace('\n',' '); }
    public String encode() throws JSONException {
        JSONArray data = new JSONArray();
        for (Entry entry : entries) data.put(new JSONObject().put("file",entry.file).put("category",entry.category)
            .put("detail",entry.detail).put("time",entry.time).put("count",entry.count));
        return data.toString();
    }
    public static FailureLog decode(String text) {
        FailureLog log = new FailureLog();
        if (text == null || text.length() > 100000) return log;
        try {
            JSONArray data = new JSONArray(text);
            for (int i = 0; i < Math.min(data.length(),LIMIT); i++) {
                JSONObject item = data.getJSONObject(i);
                log.entries.add(new Entry(bounded(item.getString("file")),bounded(item.getString("category")),bounded(item.getString("detail")),
                    item.getLong("time"),Math.max(1,Math.min(1000000,item.getInt("count")))));
            }
        } catch (JSONException | RuntimeException error) { log.clear(); }
        return log;
    }
}
