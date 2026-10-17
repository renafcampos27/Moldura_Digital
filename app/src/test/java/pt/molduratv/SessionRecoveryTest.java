package pt.molduratv;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class SessionRecoveryTest {
    private MediaEntry entry(String path) { return new MediaEntry("file://"+path,path,"video/mp4",1,10); }
    @Test public void unavailableLeftPromotesHealthyRightAndKeepsFutureOrigins() {
        MediaEntry usb=entry("/storage/USB/a.mp4"), local=entry("/storage/emulated/0/b.mp4");
        PhotoSlides.Slide future=new PhotoSlides.Slide(local,.5);
        SessionRecovery.Result result=SessionRecovery.removeVolume(Arrays.asList(new PhotoSlides.Slide(usb,.5,local,.75),future),0,"/storage/USB",false);
        assertEquals(2,result.slides.size()); assertEquals(0,result.index);
        assertSame(local,result.slides.get(0).first); assertFalse(result.slides.get(0).paired()); assertSame(future,result.slides.get(1));
    }
    @Test public void unaffectedSlidesKeepIdentitySoPlaybackPositionCanBePreserved() {
        PhotoSlides.Slide current=new PhotoSlides.Slide(entry("/storage/emulated/0/a.mp4"),.5);
        List<PhotoSlides.Slide> old=Collections.singletonList(current);
        SessionRecovery.Result result=SessionRecovery.removeVolume(old,0,"/storage/USB",false);
        assertEquals(old,result.slides); assertSame(current,result.slides.get(0));
    }
    @Test public void removalAtEndRespectsLoopAndDoesNotMatchSimilarVolumeNames() {
        PhotoSlides.Slide local=new PhotoSlides.Slide(entry("/storage/USB2/a.mp4"),.5);
        PhotoSlides.Slide gone=new PhotoSlides.Slide(entry("/storage/USB/a.mp4"),.5);
        assertEquals(-1,SessionRecovery.removeVolume(Arrays.asList(local,gone),1,"/storage/USB",false).index);
        assertEquals(0,SessionRecovery.removeVolume(Arrays.asList(local,gone),1,"/storage/USB",true).index);
        assertFalse(SessionRecovery.inside("content://provider/document/USB","/storage/USB"));
        assertTrue(SessionRecovery.inside("file:///storage/USB/F%C3%A9rias/a.mp4","/storage/USB/"));
    }
    @Test public void recoveredSourcesDoNotDuplicateExistingEntriesAndKeepBothMotionPhotoParts() {
        MediaEntry known=entry("/storage/local/a.mp4"), fresh=entry("/storage/USB/b.mp4");
        List<PhotoSlides.Slide> incoming=Collections.singletonList(new PhotoSlides.Slide(known,.5,fresh,.75));
        List<PhotoSlides.Slide> additions=SessionRecovery.onlyNew(incoming,Collections.singletonList(new PhotoSlides.Slide(known,.5)));
        assertEquals(1,additions.size()); assertSame(fresh,additions.get(0).first); assertFalse(additions.get(0).paired());
        assertTrue(SessionRecovery.onlyNew(additions,additions).isEmpty());
        MediaEntry photo=new MediaEntry("file:///motion.jpg","motion.jpg","image/jpeg",1,1000);
        List<PhotoSlides.Slide> motion=Arrays.asList(new PhotoSlides.Slide(photo,1),new PhotoSlides.Slide(photo.asMotionVideo(),1));
        assertEquals(2,SessionRecovery.onlyNew(motion,Collections.emptyList()).size());
    }
}
