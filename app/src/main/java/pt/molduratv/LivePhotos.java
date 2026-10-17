package pt.molduratv;

import java.net.URI;
import java.util.*;

/** Explicit same-folder/base-name pairing, never across folders or ambiguous exports. */
public final class LivePhotos {
    public static final int PHOTO=0,VIDEO=1,BOTH=2,SEPARATE=3;
    public static final String[] LABELS={"Só fotografia","Só vídeo associado","Fotografia e vídeo juntos","Não associar (ficheiros independentes)"};
    public static String modeName(int mode) { return LABELS[mode]; }
    private LivePhotos() {}
    public static List<MediaEntry> expand(List<MediaEntry> ordered,int mode) {
        if(mode<0 || mode>3) throw new IllegalArgumentException("Modo Live Photos inválido.");
        if(mode==SEPARATE) return new ArrayList<>(ordered);
        Map<String,List<MediaEntry>> images=new HashMap<>(),movies=new HashMap<>();
        for(MediaEntry entry:ordered) {
            String key=key(entry); if(key==null) continue;
            if(entry.isPhoto() && !entry.isRaw() && (entry.mime.equals("image/jpeg") || entry.mime.equals("image/heic") || entry.mime.equals("image/heif"))) images.computeIfAbsent(key,k->new ArrayList<>()).add(entry);
            else if(entry.mime.equals("video/quicktime")) movies.computeIfAbsent(key,k->new ArrayList<>()).add(entry);
        }
        Map<String,MediaEntry> paired=new HashMap<>(); Set<String> consumed=new HashSet<>();
        for(String key:images.keySet()) if(images.get(key).size()==1 && movies.getOrDefault(key,Collections.emptyList()).size()==1) {
            MediaEntry photo=images.get(key).get(0),video=movies.get(key).get(0); paired.put(photo.uri,video); consumed.add(video.uri);
        }
        List<MediaEntry> result=new ArrayList<>();
        for(MediaEntry entry:ordered) {
            if(consumed.contains(entry.uri)) continue;
            MediaEntry video=paired.get(entry.uri);
            if(video==null || mode!=VIDEO) result.add(entry);
            if(video!=null && mode!=PHOTO) result.add(video);
        }
        return result;
    }
    private static String key(MediaEntry entry) {
        try {
            URI uri=URI.create(entry.uri); String path=uri.getPath(); int dot=entry.name.lastIndexOf('.');
            if(path==null || dot<=0 || (!"file".equals(uri.getScheme()) && !"content".equals(uri.getScheme()))) return null;
            int slash=path.lastIndexOf('/');
            // Opaque provider IDs are not a trustworthy directory identity.
            if(slash<0 || !path.substring(slash+1).equalsIgnoreCase(entry.name)) return null;
            return uri.getScheme()+"://"+Objects.toString(uri.getAuthority(),"")+path.substring(0,slash+1)+entry.name.substring(0,dot).toLowerCase(Locale.ROOT);
        } catch(IllegalArgumentException invalid) { return null; }
    }
}
