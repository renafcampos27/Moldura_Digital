package pt.molduratv;

import org.junit.Test;
import org.json.*;
import java.util.*;
import static org.junit.Assert.*;

public class MediaExpansionTest {
    @Test public void rawCandidatesKeepSourceIdentityAndSeparatePreview() {
        for(String extension:Arrays.asList("dng","nef","nrw","arw","cr2","orf","rw2","pef","srw","raf")) {
            String mime=MediaEntry.supportedMime("test."+extension,null); MediaEntry entry=new MediaEntry("file:///original","test."+extension,mime,1,10);
            assertTrue(entry.isRaw()); assertTrue(entry.isPhoto()); assertTrue(PlaybackFilter.all().mayAccept(entry));
            MediaEntry preview=entry.withPreview("content://private/preview").withVersion(2,20);
            assertEquals(entry.uri,preview.uri); assertEquals("content://private/preview",preview.playbackUri()); assertEquals("image/jpeg",preview.playbackMime());
        }
        assertNull(MediaEntry.supportedMime("unsupported.cr3",null));
    }
    @Test public void newContainersAreClassifiedAndFilterable() {
        for(String extension:Arrays.asList("mpg","mpeg","mpe","vob","mod","flv","f4v","m2t","tod","3g2")) {
            String mime=MediaEntry.supportedMime("test."+extension,null); assertNotNull(mime); assertTrue(mime.startsWith("video/"));
            assertTrue(PlaybackFilter.all().mayAccept(new MediaEntry("file:///test","test."+extension,mime,1)));
        }
        assertEquals("video/mp4",MediaEntry.supportedMime("test.f4v",null));
    }
    @Test public void previousAllFormatsExpandsButExplicitSubsetStaysRestricted() {
        PlaybackFilter.Rule photo=new PlaybackFilter.Rule(true,Arrays.asList(PlaybackFilter.ORIENTATIONS),Arrays.asList(PlaybackFilter.RATIOS),Arrays.asList("jpeg","heif","png","webp","bmp","avif","other"),true);
        PlaybackFilter old=new PlaybackFilter(photo,PlaybackFilter.all(false)); assertTrue(old.upgradeFormats().photos.formats.contains("raw"));
        photo=new PlaybackFilter.Rule(true,photo.orientations,photo.ratios,Arrays.asList("jpeg"),true);
        assertEquals(Collections.singleton("jpeg"),new PlaybackFilter(photo,PlaybackFilter.all(false)).upgradeFormats().photos.formats);
    }
    @Test public void captureOrderAndLiveModesRoundTripAndLegacyKeepsSeparate() throws Exception {
        for(int mode=0;mode<4;mode++) {
            SettingsBackup.Data data=new SettingsBackup.Data(Collections.emptyList(),60,3,false,false,true,true,0,0,PlaybackFilter.all(),mode);
            SettingsBackup.Data restored=SettingsBackup.decode(SettingsBackup.encode(data)); assertEquals(mode,restored.liveMode); assertEquals(3,restored.order);
        }
        JSONObject legacy=new JSONObject(SettingsBackup.encode(new SettingsBackup.Data(Collections.emptyList(),60,0,false,false,true)));
        legacy.put("schemaVersion",6).getJSONObject("options").remove("livePhotoMode"); assertEquals(LivePhotos.SEPARATE,SettingsBackup.decode(legacy.toString()).liveMode);
    }
    @Test public void malformedLiveModesAndInvalidOrderAreRejected() throws Exception {
        JSONObject document=new JSONObject(SettingsBackup.encode(new SettingsBackup.Data(Collections.emptyList(),60,0,false,false,true)));
        document.getJSONObject("options").put("livePhotoMode",4); reject(document);
        document.getJSONObject("options").put("livePhotoMode","2"); reject(document);
        document.getJSONObject("options").put("livePhotoMode",2).put("order",4); reject(document);
    }
    private void reject(JSONObject document) throws Exception { try { SettingsBackup.decode(document.toString()); fail(); } catch(IllegalArgumentException | JSONException expected) { } }
}
