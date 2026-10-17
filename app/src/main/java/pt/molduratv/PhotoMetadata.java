package pt.molduratv;

import android.content.ContentResolver;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.media.MediaMetadataRetriever;
import android.os.ParcelFileDescriptor;
import androidx.exifinterface.media.ExifInterface;
import java.util.*;

/** Reads photo/video dimensions on the library worker without extracting full frames. */
final class PhotoMetadata {
    private final ContentResolver resolver;
    static final class Info {
        final double ratio; final boolean motion;
        final long captured;
        Info(double ratio, boolean motion) { this(ratio,motion,0); }
        Info(double ratio, boolean motion,long captured) { this.ratio = ratio; this.motion = motion; this.captured=captured; }
    }
    private final Map<String, Info> cache = new HashMap<>();
    private final Map<MediaEntry,Info> dirty = new HashMap<>();
    private final CollectionIndex index;
    PhotoMetadata(ContentResolver resolver) { this(resolver,null); }
    PhotoMetadata(ContentResolver resolver, CollectionIndex index) { this.resolver = resolver; this.index = index; }
    double ratio(MediaEntry entry) {
        return inspect(entry).ratio;
    }
    Info inspect(MediaEntry entry) {
        // Embedded video dimensions can differ from the still image. Do not reuse its cached ratio.
        if (entry.motionVideo) return readVideoInfo(entry);
        entry = currentVersion(entry);
        String key = entry.uri + "\n" + entry.modified + "\n" + entry.size;
        boolean versioned = entry.modified > 0 && entry.size > 0;
        Info found = versioned ? cache.get(key) : null;
        if (found != null) return found;
        found = index == null ? null : index.metadata(entry);
        if (found == null) {
            found = entry.mime.startsWith("image/") ? read(entry) : readVideoInfo(entry);
            if (versioned && (found.ratio > 0 || found.captured>0)) dirty.put(entry,found);
        }
        if (dirty.size()>=256) flush();
        if (cache.size() >= 4096) { flush(); cache.clear(); }
        if (versioned && (found.ratio > 0 || found.captured>0)) cache.put(key,found);
        return found;
    }
    private MediaEntry currentVersion(MediaEntry entry) {
        long modified=0, bytes=-1;
        try {
            Uri uri=Uri.parse(entry.uri);
            if ("file".equals(uri.getScheme())) {
                java.io.File file=new java.io.File(uri.getPath()); modified=file.lastModified(); bytes=file.length();
            } else try (android.database.Cursor cursor=resolver.query(uri,new String[]{"last_modified","_size"},null,null,null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int m=cursor.getColumnIndex("last_modified"), s=cursor.getColumnIndex("_size");
                    if (m>=0 && !cursor.isNull(m)) modified=cursor.getLong(m);
                    if (s>=0 && !cursor.isNull(s)) bytes=cursor.getLong(s);
                }
            }
        } catch (RuntimeException ignored) { }
        return entry.withVersion(modified,bytes);
    }
    void invalidate() { cache.clear(); dirty.clear(); }
    void flush() {
        if (index != null && !dirty.isEmpty()) index.saveMetadata(dirty);
        dirty.clear();
    }
    private Info readVideoInfo(MediaEntry entry) {
        try (ParcelFileDescriptor file = resolver.openFileDescriptor(Uri.parse(entry.uri), "r");
             MediaMetadataRetriever metadata = new MediaMetadataRetriever()) {
            if (file == null) return new Info(0,false);
            metadata.setDataSource(file.getFileDescriptor());
            int width = dimension(metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
            int height = dimension(metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
            String angle = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
            int rotation = dimension(angle);
            long captured=CaptureDates.video(metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE));
            return new Info(width<=0 || height<=0?0:rotation == 90 || rotation == 270 ? (double) height / width : (double) width / height,false,captured);
        } catch (java.io.IOException | RuntimeException ignored) { return new Info(0,false); }
    }
    private Info read(MediaEntry entry) {
        try (ParcelFileDescriptor file = resolver.openFileDescriptor(Uri.parse(entry.playbackUri()), "r")) {
            if (file == null) return new Info(0,false);
            BitmapFactory.Options bounds = new BitmapFactory.Options(); bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFileDescriptor(file.getFileDescriptor(), null, bounds);
            int width = bounds.outWidth, height = bounds.outHeight, rotation = 0; boolean motion = false; long captured=0;
            // A fresh descriptor avoids relying on a provider's seek position after the bounds read.
            try (ParcelFileDescriptor exifFile = resolver.openFileDescriptor(Uri.parse(entry.uri), "r")) {
                if (exifFile != null) {
                    ExifInterface exif = new ExifInterface(exifFile.getFileDescriptor());
                    if(!entry.isRaw()) {
                        width = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, width);
                        height = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, height);
                        rotation = exif.getRotationDegrees();
                    }
                    captured=CaptureDates.exif(exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL),exif.getAttribute(ExifInterface.TAG_OFFSET_TIME_ORIGINAL),exif.getAttribute(ExifInterface.TAG_SUBSEC_TIME_ORIGINAL));
                    if(captured==0) captured=CaptureDates.exif(exif.getAttribute(ExifInterface.TAG_DATETIME_DIGITIZED),exif.getAttribute(ExifInterface.TAG_OFFSET_TIME_DIGITIZED),exif.getAttribute(ExifInterface.TAG_SUBSEC_TIME_DIGITIZED));
                    long bytes = entry.size > 0 ? entry.size : exifFile.getStatSize();
                    motion = MotionPhotos.hasEmbeddedVideo(exif.getAttribute(ExifInterface.TAG_XMP),bytes);
                }
            } catch (java.io.IOException | RuntimeException ignored) { /* Bounds still usable. */ }
            if (width <= 0 || height <= 0) return new Info(0,motion,captured);
            return new Info(rotation == 90 || rotation == 270 ? (double) height / width : (double) width / height,motion,captured);
        } catch (java.io.IOException | RuntimeException ignored) { return new Info(0,false); }
    }
    private static int dimension(String value) { try { return value==null?0:Integer.parseInt(value); } catch(NumberFormatException invalid) { return 0; } }
}
