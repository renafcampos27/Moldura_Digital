package pt.molduratv;

import java.net.URI;
import java.util.*;

/** Removes only the disappeared local volume from the session; original collection/settings remain intact. */
public final class SessionRecovery {
    public static boolean inside(String uri,String volumePath) {
        try {
            URI source = URI.create(uri);
            if (!"file".equals(source.getScheme()) || volumePath == null) return false;
            String root = volumePath.replaceAll("/+$","");
            String path = source.getPath(); return path != null && (path.equals(root) || path.startsWith(root+"/"));
        } catch (IllegalArgumentException error) { return false; }
    }
    public static final class Result {
        public final List<PhotoSlides.Slide> slides;
        public final int index;
        Result(List<PhotoSlides.Slide> slides,int index) { this.slides=slides; this.index=index; }
    }
    public static Result removeVolume(List<PhotoSlides.Slide> old,int current,String path,boolean loop) {
        List<PhotoSlides.Slide> result = new ArrayList<>(); int index = 0;
        for (int i = 0; i < old.size(); i++) {
            PhotoSlides.Slide slide = old.get(i);
            boolean first = !inside(slide.first.uri,path), second = slide.second != null && !inside(slide.second.uri,path);
            if (!first && !second) continue;
            if (i < current) index++;
            if (first && (slide.second == null || second)) result.add(slide);
            else if (first) result.add(new PhotoSlides.Slide(slide.first,slide.firstRatio));
            else result.add(new PhotoSlides.Slide(slide.second,slide.secondRatio));
        }
        if (index >= result.size()) index = loop && !result.isEmpty() ? 0 : -1;
        return new Result(result,index);
    }
    public static List<PhotoSlides.Slide> onlyNew(List<PhotoSlides.Slide> incoming,List<PhotoSlides.Slide> existing) {
        Set<String> known=new HashSet<>();
        for(PhotoSlides.Slide slide:existing) { known.add(slide.first.uri); if(slide.paired()) known.add(slide.second.uri); }
        List<PhotoSlides.Slide> result=new ArrayList<>();
        for(PhotoSlides.Slide slide:incoming) {
            boolean first=!known.contains(slide.first.uri), second=slide.paired() && !known.contains(slide.second.uri);
            if(first && (slide.second==null || second)) result.add(slide);
            else if(first) result.add(new PhotoSlides.Slide(slide.first,slide.firstRatio));
            else if(second) result.add(new PhotoSlides.Slide(slide.second,slide.secondRatio));
        }
        return result;
    }
}
