package pt.molduratv;

import java.util.*;
import org.json.*;

/** Immutable filters: OR within a selection, AND between dimensions; photo/video rules are independent. */
public final class PlaybackFilter {
    public static final String[] ORIENTATIONS={"horizontal","vertical","square","unknown"};
    public static final String[] ORIENTATION_LABELS={"Horizontal","Vertical","Quadrado","Orientação desconhecida"};
    public static final String[] RATIOS={"16:9","9:16","4:3","3:4","3:2","2:3","1:1","9:20","9:21","21:9","other","unknown"};
    public static final String[] RATIO_LABELS={"16:9","9:16","4:3","3:4","3:2","2:3","1:1","9:20","9:21","21:9","Outras proporções","Proporção desconhecida"};
    public static final String[] PHOTO_FORMATS={"jpeg","heif","png","webp","bmp","avif","raw","other"};
    public static final String[] PHOTO_LABELS={"JPEG / JPG","HEIC / HEIF","PNG","WebP","BMP","AVIF","RAW (pré-visualização JPEG)","Outros formatos"};
    public static final String[] VIDEO_FORMATS={"mp4","mov","mkv","webm","ts","avi","3gp","mpegps","flv","motion","other"};
    public static final String[] VIDEO_LABELS={"MP4 / M4V / F4V","MOV","MKV","WebM","TS / MTS / M2TS / M2T / TOD","AVI","3GP / 3G2","MPEG / MPG / VOB / MOD","FLV","Vídeo incorporado em Motion Photo","Outros formatos"};
    public static final class Rule {
        public final boolean enabled;
        public final Set<String> orientations,ratios,formats;
        public Rule(boolean enabled,Collection<String> orientations,Collection<String> ratios,Collection<String> formats,boolean photo) {
            this.enabled=enabled; this.orientations=selection(orientations,ORIENTATIONS);
            this.ratios=selection(ratios,RATIOS); this.formats=selection(formats,photo?PHOTO_FORMATS:VIDEO_FORMATS);
        }
        private static Set<String> selection(Collection<String> values,String[] allowed) {
            if(values==null || values.size()>allowed.length || !Arrays.asList(allowed).containsAll(values)) throw new IllegalArgumentException("Filtro inválido.");
            return Collections.unmodifiableSet(new LinkedHashSet<>(values));
        }
    }
    public final Rule photos,videos;
    public PlaybackFilter(Rule photos,Rule videos) { this.photos=Objects.requireNonNull(photos); this.videos=Objects.requireNonNull(videos); }
    public static Rule all(boolean photo) { return new Rule(true,Arrays.asList(ORIENTATIONS),Arrays.asList(RATIOS),Arrays.asList(photo?PHOTO_FORMATS:VIDEO_FORMATS),photo); }
    public static PlaybackFilter all() { return new PlaybackFilter(all(true),all(false)); }
    public PlaybackFilter with(boolean photo,Rule rule) { return new PlaybackFilter(photo?rule:photos,photo?videos:rule); }
    public boolean mayAccept(MediaEntry entry) { Rule rule=entry.isPhoto()?photos:videos; return rule.enabled && rule.formats.contains(format(entry)); }
    public boolean needsRatio(MediaEntry entry) { Rule rule=entry.isPhoto()?photos:videos; return rule.orientations.size()!=ORIENTATIONS.length || rule.ratios.size()!=RATIOS.length; }
    public boolean accepts(MediaEntry entry,double ratio) {
        Rule rule=entry.isPhoto()?photos:videos;
        String orientation=!(ratio>0) || !Double.isFinite(ratio)?"unknown":ratio>1.02?"horizontal":ratio<1/1.02?"vertical":"square";
        return mayAccept(entry) && rule.orientations.contains(orientation) && rule.ratios.contains(ratioKey(ratio));
    }
    public static String ratioKey(double ratio) {
        if(!(ratio>0) || !Double.isFinite(ratio)) return "unknown";
        String best="other"; double distance=.025;
        for(String key:RATIOS) if(key.contains(":")) {
            String[] parts=key.split(":"); double value=Double.parseDouble(parts[0])/Double.parseDouble(parts[1]);
            double delta=Math.abs(Math.log(ratio/value)); if(delta<distance) { distance=delta; best=key; }
        }
        return best;
    }
    private static String format(MediaEntry entry) {
        if(entry.motionVideo) return "motion";
        if(entry.isRaw()) return "raw";
        return switch(entry.mime) {
            case "image/jpeg" -> "jpeg"; case "image/heic","image/heif" -> "heif";
            case "image/png" -> "png"; case "image/webp" -> "webp"; case "image/bmp" -> "bmp"; case "image/avif" -> "avif";
            case "video/mp4" -> "mp4"; case "video/quicktime" -> "mov"; case "video/x-matroska" -> "mkv";
            case "video/webm" -> "webm"; case "video/mp2t" -> "ts"; case "video/x-msvideo" -> "avi"; case "video/3gpp","video/3gpp2" -> "3gp";
            case "video/mp2p" -> "mpegps"; case "video/x-flv" -> "flv";
            default -> "other";
        };
    }
    public JSONObject json() throws JSONException { return new JSONObject().put("photos",json(photos)).put("videos",json(videos)); }
    private static JSONObject json(Rule rule) throws JSONException {
        return new JSONObject().put("enabled",rule.enabled).put("orientations",new JSONArray(rule.orientations)).put("ratios",new JSONArray(rule.ratios)).put("formats",new JSONArray(rule.formats));
    }
    public static PlaybackFilter read(JSONObject object) throws JSONException { return new PlaybackFilter(read(object.getJSONObject("photos"),true),read(object.getJSONObject("videos"),false)); }
    public PlaybackFilter upgradeFormats() {
        return new PlaybackFilter(upgrade(photos,true),upgrade(videos,false));
    }
    private static Rule upgrade(Rule rule,boolean photo) {
        Set<String> old=new LinkedHashSet<>(Arrays.asList(photo?PHOTO_FORMATS:VIDEO_FORMATS));
        old.remove("raw"); old.remove("mpegps"); old.remove("flv");
        return rule.formats.equals(old)?new Rule(rule.enabled,rule.orientations,rule.ratios,Arrays.asList(photo?PHOTO_FORMATS:VIDEO_FORMATS),photo):rule;
    }
    private static Rule read(JSONObject object,boolean photo) throws JSONException {
        Object enabled=object.get("enabled"); if(!(enabled instanceof Boolean)) throw new IllegalArgumentException("Filtro inválido.");
        return new Rule((Boolean)enabled,strings(object.getJSONArray("orientations")),strings(object.getJSONArray("ratios")),strings(object.getJSONArray("formats")),photo);
    }
    private static List<String> strings(JSONArray array) throws JSONException {
        if(array.length()>32) throw new IllegalArgumentException("Filtro inválido.");
        List<String> result=new ArrayList<>(); for(int i=0;i<array.length();i++) { Object value=array.get(i); if(!(value instanceof String) || result.contains(value)) throw new IllegalArgumentException("Filtro inválido."); result.add((String)value); } return result;
    }
    public String summary() { return "Fotografias: "+summary(photos,true)+" · Vídeos: "+summary(videos,false); }
    private static String summary(Rule rule,boolean photo) {
        if(!rule.enabled) return "desligadas/os";
        if(rule.orientations.isEmpty() || rule.ratios.isEmpty() || rule.formats.isEmpty()) return "nenhum (seleção vazia)";
        return rule.orientations.size()==ORIENTATIONS.length && rule.ratios.size()==RATIOS.length && rule.formats.size()==(photo?PHOTO_FORMATS:VIDEO_FORMATS).length?"todos":"filtrados";
    }
}
