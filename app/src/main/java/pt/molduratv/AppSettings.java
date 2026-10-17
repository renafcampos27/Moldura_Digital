package pt.molduratv;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import java.util.*;

public final class AppSettings {
    private final SharedPreferences prefs;
    public final List<String> roots = new ArrayList<>();
    public int seconds, order, layoutMode, motionMode, liveMode;
    public PlaybackFilter filter = PlaybackFilter.all();
    public boolean crop, muted, loop, smartPhotos;
    public AppSettings(Context context) {
        prefs = context.getSharedPreferences("collection", Context.MODE_PRIVATE);
        seconds = prefs.getInt("seconds", 60); order = prefs.getInt("order", 0);
        if(order<0 || order>3) order=0;
        crop = prefs.getBoolean("crop", false); muted = prefs.getBoolean("muted", false);
        loop = prefs.getBoolean("loop", true);
        smartPhotos = prefs.getBoolean("smartPhotos", true);
        layoutMode = prefs.getInt("layoutMode", smartPhotos ? PhotoSlides.AUTOMATIC : PhotoSlides.INDIVIDUAL);
        if (layoutMode < 0 || layoutMode > 2) layoutMode = PhotoSlides.AUTOMATIC;
        smartPhotos = layoutMode != PhotoSlides.INDIVIDUAL;
        motionMode = prefs.getInt("motionMode",MotionPhotos.PHOTO);
        liveMode=prefs.getInt("liveMode",LivePhotos.BOTH);
        if(liveMode<0 || liveMode>3) liveMode=LivePhotos.BOTH;
        if (motionMode < 0 || motionMode > 2) motionMode=MotionPhotos.PHOTO;
        try { filter=PlaybackFilter.read(new org.json.JSONObject(prefs.getString("playbackFilter", "{}"))); }
        catch (org.json.JSONException | IllegalArgumentException ignored) { filter=PlaybackFilter.all(); }
        if(prefs.getInt("filterRevision",0)<1) filter=filter.upgradeFormats();
        try {
            JSONArray values = new JSONArray(prefs.getString("roots", "[]"));
            for (int i = 0; i < values.length(); i++) roots.add(values.getString(i));
        } catch (org.json.JSONException ignored) { /* Start with an empty collection. */ }
    }
    public void save() {
        try { prefs.edit().putString("playbackFilter",filter.json().toString()).putInt("filterRevision",1).apply(); }
        catch (org.json.JSONException error) { throw new IllegalStateException(error); }
        smartPhotos = layoutMode != PhotoSlides.INDIVIDUAL;
        prefs.edit().putString("roots", new JSONArray(roots).toString())
            .putInt("seconds", seconds).putInt("order", order).putBoolean("crop", crop)
            .putBoolean("muted", muted).putBoolean("loop", loop).putBoolean("smartPhotos", smartPhotos)
            .putInt("layoutMode", layoutMode).putInt("motionMode",motionMode).putInt("liveMode",liveMode).apply();
    }
    public SettingsBackup.Data snapshot() {
        return new SettingsBackup.Data(roots, seconds, order, crop, muted, loop, smartPhotos, layoutMode,motionMode,filter,liveMode);
    }
    public void restore(SettingsBackup.Data data) {
        roots.clear(); roots.addAll(data.roots);
        seconds = data.seconds; order = data.order;
        crop = data.crop; muted = data.muted; loop = data.loop;
        smartPhotos = data.smartPhotos;
        layoutMode = data.layoutMode;
        motionMode = data.motionMode;
        filter = data.filter;
        liveMode=data.liveMode;
        save();
    }
}
