package pt.molduratv;

import org.junit.Test;
import org.json.*;
import java.util.*;
import static org.junit.Assert.*;

public class PlaybackFilterTest {
    private MediaEntry photo=new MediaEntry("file:///photo.jpg","photo.jpg","image/jpeg",1,100);
    private MediaEntry video=new MediaEntry("file:///video.mp4","video.mp4","video/mp4",1,100);
    private PlaybackFilter.Rule rule(boolean enabled,List<String> orientations,List<String> ratios,List<String> formats,boolean photo) { return new PlaybackFilter.Rule(enabled,orientations,ratios,formats,photo); }
    @Test public void defaultsIncludeAllAndUnknownWithoutMetadata() {
        PlaybackFilter filter=PlaybackFilter.all();
        assertTrue(filter.accepts(photo,0)); assertTrue(filter.accepts(video,16d/9)); assertFalse(filter.needsRatio(photo));
    }
    @Test public void independentTypesAndCombinedSelections() {
        PlaybackFilter filter=new PlaybackFilter(rule(true,Arrays.asList("horizontal","vertical"),Arrays.asList("4:3","3:4"),Arrays.asList("jpeg"),true),
            rule(true,Arrays.asList("vertical"),Arrays.asList(PlaybackFilter.RATIOS),Arrays.asList("mp4"),false));
        assertTrue(filter.accepts(photo,4d/3)); assertTrue(filter.accepts(photo,3d/4)); assertFalse(filter.accepts(photo,16d/9));
        assertTrue(filter.accepts(video,9d/20)); assertFalse(filter.accepts(video,16d/9)); assertFalse(filter.accepts(video,0));
        assertFalse(filter.accepts(new MediaEntry("x","x.png","image/png",1),4d/3)); assertTrue(filter.needsRatio(video));
    }
    @Test public void disablingAndEmptySelectionsBlockType() {
        PlaybackFilter filter=PlaybackFilter.all().with(true,rule(false,Arrays.asList(PlaybackFilter.ORIENTATIONS),Arrays.asList(PlaybackFilter.RATIOS),Arrays.asList(PlaybackFilter.PHOTO_FORMATS),true));
        assertFalse(filter.accepts(photo,1)); assertTrue(filter.accepts(video,1));
        filter=filter.with(false,rule(true,Collections.emptyList(),Arrays.asList(PlaybackFilter.RATIOS),Arrays.asList(PlaybackFilter.VIDEO_FORMATS),false)); assertFalse(filter.accepts(video,1));
    }
    @Test public void ratioToleranceDistinguishesPortraitFormatsAndUnknown() {
        assertEquals("4:3",PlaybackFilter.ratioKey(4032d/3024)); assertEquals("9:16",PlaybackFilter.ratioKey(1080d/1920));
        assertEquals("9:20",PlaybackFilter.ratioKey(1080d/2400)); assertEquals("9:21",PlaybackFilter.ratioKey(1080d/2520));
        assertEquals("other",PlaybackFilter.ratioKey(5)); assertEquals("unknown",PlaybackFilter.ratioKey(Double.NaN));
    }
    @Test public void motionUsesVideoRuleAndDedicatedFormat() {
        PlaybackFilter filter=PlaybackFilter.all().with(false,rule(true,Arrays.asList(PlaybackFilter.ORIENTATIONS),Arrays.asList(PlaybackFilter.RATIOS),Arrays.asList("mp4"),false));
        assertTrue(filter.accepts(photo,1)); assertFalse(filter.accepts(photo.asMotionVideo(),1)); assertTrue(filter.accepts(video,1));
    }
    @Test public void filterBackupRoundTripAndOldSchemaDefaults() throws Exception {
        PlaybackFilter filter=PlaybackFilter.all().with(true,rule(true,Arrays.asList("vertical"),Arrays.asList("3:4","9:16"),Arrays.asList("jpeg","heif"),true));
        SettingsBackup.Data data=new SettingsBackup.Data(Collections.emptyList(),60,0,false,false,true,true,0,0,filter);
        JSONObject document=new JSONObject(SettingsBackup.encode(data));
        assertEquals(filter.json().toString(),SettingsBackup.decode(document.toString()).filter.json().toString());
        document.put("schemaVersion",5).getJSONObject("options").remove("playbackFilter");
        assertTrue(SettingsBackup.decode(document.toString()).filter.accepts(photo,16d/9));
    }
    @Test public void invalidBackupFilterRejectedBeforeRestore() throws Exception {
        JSONObject json=PlaybackFilter.all().json(); json.getJSONObject("photos").put("enabled","true"); reject(json);
        json=PlaybackFilter.all().json(); json.getJSONObject("videos").put("ratios",new JSONArray(Arrays.asList("9:99"))); reject(json);
        json=PlaybackFilter.all().json(); json.getJSONObject("videos").put("formats",new JSONArray(Arrays.asList("mp4","mp4"))); reject(json);
    }
    private void reject(JSONObject json) { try { PlaybackFilter.read(json); fail("Should reject"); } catch(JSONException | IllegalArgumentException expected) { } }
}
