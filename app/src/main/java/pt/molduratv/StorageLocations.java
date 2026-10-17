package pt.molduratv;

import android.content.Context;
import android.os.*;
import android.os.storage.*;
import java.io.*;
import java.util.*;

public final class StorageLocations {
    public static final class Location {
        public final String label;
        public final File directory;
        public final boolean appOwned;
        Location(String label, File directory, boolean appOwned) { this.label = label; this.directory = directory; this.appOwned = appOwned; }
    }
    private StorageLocations() {}
    @SuppressWarnings("deprecation") // The shared storage root remains necessary for a user-authorized file browser on Android 8–10.
    public static List<Location> discover(Context context) {
        Map<String, Location> result = new LinkedHashMap<>();
        add(result, "Armazenamento interno", Environment.getExternalStorageDirectory(), false);
        add(result, "Pasta privada da MolduraTV", context.getFilesDir(), true);
        StorageManager manager = context.getSystemService(StorageManager.class);
        if (Build.VERSION.SDK_INT >= 30 && manager != null) {
            try {
                for (StorageVolume volume : manager.getStorageVolumes()) {
                    if (Environment.MEDIA_MOUNTED.equals(volume.getState()) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(volume.getState()))
                        add(result, volume.isPrimary() ? "Armazenamento interno" : volume.getDescription(context), volume.getDirectory(), false);
                }
            } catch (RuntimeException ignored) { /* Keep private folders and the fallback catalog usable on vendor firmware. */ }
        }
        File[] appDirectories;
        try { appDirectories = context.getExternalFilesDirs(null); }
        catch (RuntimeException error) { appDirectories = new File[0]; }
        if (appDirectories == null) appDirectories = new File[0];
        for (int i = 0; i < appDirectories.length; i++) {
            File owned = appDirectories[i];
            if (owned == null) continue;
            add(result, "Pasta da MolduraTV · volume " + (i + 1), owned, true);
            File volume = owned;
            for (int parent = 0; parent < 4 && volume != null; parent++) volume = volume.getParentFile();
            add(result, "Armazenamento · " + (volume == null ? "externo" : volume.getName()), volume, false);
        }
        File[] mounted = new File("/storage").listFiles();
        if (mounted != null) for (File candidate : mounted) {
            if (candidate.isDirectory() && !Arrays.asList("emulated", "self").contains(candidate.getName()))
                add(result, "Disco / cartão · " + candidate.getName(), candidate, false);
        }
        return new ArrayList<>(result.values());
    }
    private static void add(Map<String, Location> output, String label, File directory, boolean owned) {
        if (directory == null) return;
        try {
            String path = directory.getCanonicalPath();
            output.putIfAbsent(path, new Location(label, new File(path), owned));
        } catch (IOException | SecurityException ignored) { }
    }
}
