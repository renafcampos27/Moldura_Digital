package pt.molduratv;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class LivePhotosTest {
    private MediaEntry entry(String directory,String name,String mime) { return new MediaEntry("file:///"+directory+"/"+name,name,mime,1); }
    private final MediaEntry photo=entry("album","IMG_001.HEIC","image/heic"),movie=entry("album","IMG_001.MOV","video/quicktime"),other=entry("album","other.jpg","image/jpeg");
    @Test public void groupsAtPhotoPositionWithoutRepeatingCompanion() {
        assertEquals(Arrays.asList(photo,movie,other),LivePhotos.expand(Arrays.asList(movie,photo,other),LivePhotos.BOTH));
    }
    @Test public void stillOnlyAndVideoOnlyKeepUnmatchedOriginals() {
        assertEquals(Arrays.asList(photo,other),LivePhotos.expand(Arrays.asList(photo,other,movie),LivePhotos.PHOTO));
        assertEquals(Arrays.asList(movie,other),LivePhotos.expand(Arrays.asList(photo,other,movie),LivePhotos.VIDEO));
    }
    @Test public void separatePreservesOriginalOrderAndInput() {
        List<MediaEntry> input=Arrays.asList(movie,other,photo); assertEquals(input,LivePhotos.expand(input,LivePhotos.SEPARATE));
        LivePhotos.expand(input,LivePhotos.BOTH); assertEquals(movie,input.get(0));
    }
    @Test public void rejectsCrossDirectoryAndAmbiguousPhotoNames() {
        MediaEntry away=entry("other","IMG_001.MOV","video/quicktime"),duplicate=entry("album","IMG_001.JPG","image/jpeg");
        assertEquals(Arrays.asList(photo,away),LivePhotos.expand(Arrays.asList(photo,away),LivePhotos.PHOTO));
        assertEquals(Arrays.asList(photo,duplicate,movie),LivePhotos.expand(Arrays.asList(photo,duplicate,movie),LivePhotos.PHOTO));
    }
    @Test public void associatesHierarchicalDocumentUrisButNotOpaqueIds() {
        MediaEntry a=new MediaEntry("content://files/tree/root/document/primary%3ADcim%2FIMG_1.jpg","IMG_1.jpg","image/jpeg",1);
        MediaEntry b=new MediaEntry("content://files/tree/root/document/primary%3ADcim%2FIMG_1.mov","IMG_1.mov","video/quicktime",1);
        assertEquals(Collections.singletonList(a),LivePhotos.expand(Arrays.asList(a,b),LivePhotos.PHOTO));
        a=new MediaEntry("content://files/document/123","IMG_1.jpg","image/jpeg",1); b=new MediaEntry("content://files/document/124","IMG_1.mov","video/quicktime",1);
        assertEquals(2,LivePhotos.expand(Arrays.asList(a,b),LivePhotos.PHOTO).size());
    }
    @Test public void doesNotConsumeRawOrUnrelatedMp4() {
        MediaEntry raw=entry("album","IMG_001.dng","image/x-molduratv-raw"),mp4=entry("album","IMG_001.mp4","video/mp4");
        assertEquals(3,LivePhotos.expand(Arrays.asList(raw,movie,mp4),LivePhotos.VIDEO).size());
    }
}
