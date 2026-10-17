package pt.molduratv;

import java.util.Locale;

public final class MediaEntry {
    public final String uri, name, mime;
    public final long modified, size;
    public final boolean motionVideo;
    public final String previewUri;
    public MediaEntry(String uri, String name, String mime, long modified) {
        this(uri,name,mime,modified,-1,false);
    }
    public MediaEntry(String uri, String name, String mime, long modified, long size) {
        this(uri,name,mime,modified,size,false);
    }
    public MediaEntry(String uri, String name, String mime, long modified, long size, boolean motionVideo) {
        this(uri,name,mime,modified,size,motionVideo,null);
    }
    private MediaEntry(String uri, String name, String mime, long modified, long size, boolean motionVideo,String previewUri) {
        this.uri = uri; this.name = name; this.mime = mime; this.modified = modified; this.size = size; this.motionVideo = motionVideo;
        this.previewUri=previewUri;
    }
    public boolean isRaw() { return mime.equals("image/x-molduratv-raw"); }
    public String playbackUri() { return previewUri==null?uri:previewUri; }
    public String playbackMime() { return isRaw()?"image/jpeg":mime; }
    public MediaEntry withPreview(String preview) { return new MediaEntry(uri,name,mime,modified,size,motionVideo,preview); }
    public MediaEntry withVersion(long date,long bytes) { return new MediaEntry(uri,name,mime,date,bytes,motionVideo,previewUri); }
    public boolean isPhoto() { return mime.startsWith("image/") && !motionVideo; }
    public MediaEntry asMotionVideo() { return new MediaEntry(uri,name + " · movimento",mime,modified,size,true); }
    public static String supportedMime(String name, String declared) {
        String n = name.toLowerCase(Locale.ROOT);
        String ext = n.substring(n.lastIndexOf('.') + 1);
        return switch (ext) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "heic" -> "image/heic";
            case "heif" -> "image/heif";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            case "bmp" -> "image/bmp";
            case "avif" -> "image/avif";
            case "dng", "nef", "nrw", "arw", "cr2", "orf", "rw2", "pef", "srw", "raf" -> "image/x-molduratv-raw";
            case "mp4", "m4v", "f4v" -> "video/mp4";
            case "mov" -> "video/quicktime";
            case "mkv" -> "video/x-matroska";
            case "webm" -> "video/webm";
            case "ts", "mts", "m2ts" -> "video/mp2t";
            case "avi" -> "video/x-msvideo";
            case "3gp" -> "video/3gpp";
            case "3g2" -> "video/3gpp2";
            case "mpg", "mpeg", "mpe", "vob", "mod" -> "video/mp2p";
            case "flv" -> "video/x-flv";
            case "m2t", "tod" -> "video/mp2t";
            default -> declared != null && declared.startsWith("video/") ? declared : null;
        };
    }
}
