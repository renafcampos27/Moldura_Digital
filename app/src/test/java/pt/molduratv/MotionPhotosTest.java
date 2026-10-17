package pt.molduratv;
import org.junit.Test;
import static org.junit.Assert.*;
public class MotionPhotosTest {
    private String xmp(String attrs,String items) {
        return "<x:xmpmeta xmlns:x='adobe:ns:meta/' xmlns:GCamera='http://ns.google.com/photos/1.0/camera/' xmlns:Item='http://ns.google.com/photos/1.0/container/item/'><description "+attrs+">"+items+"</description></x:xmpmeta>";
    }
    @Test public void recognizesLegacyMicroVideoOnlyWithValidFlagAndOffset() {
        assertTrue(MotionPhotos.hasEmbeddedVideo(xmp("GCamera:MicroVideo='1' GCamera:MicroVideoOffset='100'",""),1000));
        assertFalse(MotionPhotos.hasEmbeddedVideo(xmp("GCamera:MicroVideo='0' GCamera:MicroVideoOffset='100'",""),1000));
        for (String offset : new String[]{"-1","0","1000","9223372036854775808"})
            assertFalse(MotionPhotos.hasEmbeddedVideo(xmp("GCamera:MicroVideo='1' GCamera:MicroVideoOffset='"+offset+"'",""),1000));
    }
    @Test public void recognizesDirectoryWithPrimaryStillAndEmbeddedMp4() {
        String items="<item Item:Mime='image/jpeg' Item:Semantic='Primary'/><item Item:Mime='video/mp4' Item:Semantic='MotionPhoto' Item:Length='200'/>";
        assertTrue(MotionPhotos.hasEmbeddedVideo(xmp("GCamera:MotionPhoto='1'",items),1000));
        assertFalse(MotionPhotos.hasEmbeddedVideo(xmp("GCamera:MotionPhoto='1'",items.replace("200","2000")),1000));
        assertFalse(MotionPhotos.hasEmbeddedVideo(xmp("GCamera:MotionPhoto='1'",items.replace("video/mp4","video/other")),1000));
    }
    @Test public void ordinaryImagesMalformedXmlAndExternalEntitiesDoNotBecomeVideos() {
        assertFalse(MotionPhotos.hasEmbeddedVideo(null,1000)); assertFalse(MotionPhotos.hasEmbeddedVideo("<broken",1000));
        assertFalse(MotionPhotos.hasEmbeddedVideo("<!DOCTYPE x SYSTEM 'file:///private'><x/>",1000));
        assertFalse(MotionPhotos.hasEmbeddedVideo(xmp("GCamera:MotionPhoto='1' GCamera:MicroVideoOffset='100'","").replace("http://ns.google.com/photos/1.0/camera/","other"),1000));
    }
    @Test public void motionEntryKeepsOriginalSourceButBehavesAsVideo() {
        MediaEntry photo=new MediaEntry("file:///a.jpg","a.jpg","image/jpeg",5,1000);
        MediaEntry video=photo.asMotionVideo();
        assertTrue(photo.isPhoto()); assertFalse(video.isPhoto()); assertTrue(video.motionVideo);
        assertEquals(photo.uri,video.uri); assertEquals(photo.mime,video.mime); assertEquals(photo.size,video.size);
    }
}
