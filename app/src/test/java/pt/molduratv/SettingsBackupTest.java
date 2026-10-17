package pt.molduratv;

import org.json.*;
import org.junit.Test;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.Assert.*;

public class SettingsBackupTest {
    @org.junit.Rule public org.junit.rules.TemporaryFolder temporary = new org.junit.rules.TemporaryFolder();
    private SettingsBackup.Data sample() {
        return new SettingsBackup.Data(Arrays.asList("content://storage.provider/tree/primary%3AF%C3%A9rias", "content://nas.provider/tree/family"),
            120, 2, true, true, false);
    }
    @Test public void roundTripPreservesFoldersAndEveryOption() throws Exception {
        String text = SettingsBackup.encode(sample());
        SettingsBackup.Data restored = SettingsBackup.read(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
        assertEquals(sample().roots, restored.roots); assertEquals(120, restored.seconds); assertEquals(2, restored.order);
        assertTrue(restored.crop); assertTrue(restored.muted); assertFalse(restored.loop);
    }
    @Test public void emptyCollectionAndDuplicateFoldersAreHandled() throws Exception {
        SettingsBackup.Data empty = new SettingsBackup.Data(Collections.emptyList(), 60, 0, false, false, true);
        assertTrue(SettingsBackup.decode(SettingsBackup.encode(empty)).roots.isEmpty());
        SettingsBackup.Data duplicate = new SettingsBackup.Data(Arrays.asList(sample().roots.get(0), sample().roots.get(0)), 60, 0, false, false, true);
        assertEquals(1, duplicate.roots.size());
    }
    @Test public void rejectsWrongFormatFutureSchemaAndTrailingContent() throws Exception {
        JSONObject object = new JSONObject(SettingsBackup.encode(sample()));
        object.put("schemaVersion", 99); reject(object.toString());
        object.put("schemaVersion", 2).put("format", "other-app"); reject(object.toString());
        reject(SettingsBackup.encode(sample()) + "garbage"); reject("not json");
        StringBuilder nested = new StringBuilder();
        for (int i = 0; i < 1000; i++) nested.append('[');
        reject(nested.toString());
    }
    @Test public void rejectsMissingOrCoercedOptions() throws Exception {
        JSONObject object = new JSONObject(SettingsBackup.encode(sample()));
        object.getJSONObject("options").put("loop", "true"); reject(object.toString());
        object = new JSONObject(SettingsBackup.encode(sample()));
        object.getJSONObject("options").put("photoSeconds", "60"); reject(object.toString());
        object = new JSONObject(SettingsBackup.encode(sample()));
        object.getJSONObject("options").put("photoSeconds", 60.5); reject(object.toString());
        object = new JSONObject(SettingsBackup.encode(sample()));
        object.getJSONObject("options").put("photoSeconds", 0); reject(object.toString());
        object = new JSONObject(SettingsBackup.encode(sample()));
        object.getJSONObject("options").remove("crop"); reject(object.toString());
    }
    @Test public void rejectsUrlsAndNonTreeReferences() throws Exception {
        for (String uri : Arrays.asList("file:relative", "file://server/private", "file:///storage/../data", "https://example.com/folder", "content://provider/document/item", "content://provider/tree/", "content://provider/tree/root?token=secret")) {
            JSONObject object = new JSONObject(SettingsBackup.encode(sample()));
            object.put("folders", new JSONArray(Collections.singletonList(uri))); reject(object.toString());
        }
    }
    @Test public void restoresLegacyDocumentBackupsAndMixedLocalCollections() throws Exception {
        JSONObject legacy = new JSONObject(SettingsBackup.encode(sample())).put("schemaVersion", 1);
        assertEquals(sample().roots, SettingsBackup.decode(legacy.toString()).roots);
        legacy.getJSONArray("folders").put("file:///storage/emulated/0/Pictures"); reject(legacy.toString());
        List<String> mixed = Arrays.asList("file:///storage/emulated/0/Pictures", "content://nas.provider/tree/family");
        SettingsBackup.Data data = new SettingsBackup.Data(mixed, 60, 0, false, false, true);
        assertEquals(mixed, SettingsBackup.decode(SettingsBackup.encode(data)).roots);
        assertEquals(7, new JSONObject(SettingsBackup.encode(data)).getInt("schemaVersion"));
    }
    @Test public void smartPhotoModeRoundTripsAndLegacyBackupsDefaultToEnabled() throws Exception {
        SettingsBackup.Data disabled = new SettingsBackup.Data(sample().roots, 60, 0, false, false, true, false);
        assertFalse(SettingsBackup.decode(SettingsBackup.encode(disabled)).smartPhotos);
        JSONObject legacy = new JSONObject(SettingsBackup.encode(disabled));
        legacy.getJSONObject("options").remove("smartPhotos");
        reject(legacy.toString());
        for (int schema : new int[]{1, 2}) {
            legacy.put("schemaVersion", schema);
            assertTrue(SettingsBackup.decode(legacy.toString()).smartPhotos);
        }
        legacy.put("schemaVersion", 3).getJSONObject("options").put("smartPhotos", "true");
        reject(legacy.toString());
    }
    @Test public void createsReadableLocalBackupAndPreservesExistingBackupOnCollision() throws Exception {
        File target = new File(temporary.getRoot(), "backup.json");
        SettingsBackup.writeNew(target, sample());
        try (InputStream input = new FileInputStream(target)) { assertEquals(sample().roots, SettingsBackup.read(input).roots); }
        byte[] original = java.nio.file.Files.readAllBytes(target.toPath());
        try {
            SettingsBackup.writeNew(target, new SettingsBackup.Data(Collections.emptyList(), 5, 0, false, false, false)); fail();
        } catch (java.nio.file.FileAlreadyExistsException expected) { }
        assertArrayEquals(original, java.nio.file.Files.readAllBytes(target.toPath()));
    }
    @Test public void presentationModesRoundTripAndOldSchemasMigrateWithoutEnablingDisabledMode() throws Exception {
        for (int mode : new int[]{PhotoSlides.AUTOMATIC, PhotoSlides.INDIVIDUAL, PhotoSlides.PAIRS}) {
            SettingsBackup.Data data = new SettingsBackup.Data(sample().roots,60,0,false,false,true,mode != PhotoSlides.INDIVIDUAL,mode);
            assertEquals(mode,SettingsBackup.decode(SettingsBackup.encode(data)).layoutMode);
        }
        JSONObject legacy = new JSONObject(SettingsBackup.encode(new SettingsBackup.Data(sample().roots,60,0,false,false,true,false)));
        legacy.put("schemaVersion",3).getJSONObject("options").remove("presentationMode");
        assertEquals(PhotoSlides.INDIVIDUAL,SettingsBackup.decode(legacy.toString()).layoutMode);
        legacy.getJSONObject("options").put("smartPhotos",true);
        assertEquals(PhotoSlides.AUTOMATIC,SettingsBackup.decode(legacy.toString()).layoutMode);
    }
    @Test public void rejectsMissingInvalidOrContradictoryPresentationModes() throws Exception {
        JSONObject document = new JSONObject(SettingsBackup.encode(sample()));
        document.getJSONObject("options").remove("presentationMode"); reject(document.toString());
        for (Object invalid : new Object[]{-1,3,"0",0.5}) {
            document.getJSONObject("options").put("presentationMode",invalid); reject(document.toString());
        }
        document.getJSONObject("options").put("presentationMode",PhotoSlides.INDIVIDUAL); reject(document.toString());
    }
    @Test public void motionModesRoundTripAndPreviousSchemasKeepStillPhotoDefault() throws Exception {
        for(int mode=0;mode<=2;mode++) {
            SettingsBackup.Data data=new SettingsBackup.Data(sample().roots,60,0,false,false,true,true,0,mode);
            assertEquals(mode,SettingsBackup.decode(SettingsBackup.encode(data)).motionMode);
        }
        JSONObject legacy=new JSONObject(SettingsBackup.encode(sample()));
        legacy.getJSONObject("options").remove("motionPhotoMode"); reject(legacy.toString());
        legacy.put("schemaVersion",4); assertEquals(MotionPhotos.PHOTO,SettingsBackup.decode(legacy.toString()).motionMode);
        legacy.put("schemaVersion",5).getJSONObject("options").put("motionPhotoMode",3); reject(legacy.toString());
    }
    @Test public void rejectsOversizedInputAndExcessiveFolderCount() throws Exception {
        try { SettingsBackup.read(new ByteArrayInputStream(new byte[SettingsBackup.MAX_BYTES + 1])); fail(); }
        catch (IOException expected) { }
        JSONObject object = new JSONObject(SettingsBackup.encode(sample()));
        JSONArray roots = new JSONArray();
        for (int i = 0; i <= SettingsBackup.MAX_FOLDERS; i++) roots.put("content://provider/tree/" + i);
        object.put("folders", roots); reject(object.toString());
    }
    private void reject(String text) throws Exception {
        try { SettingsBackup.decode(text); fail("Invalid backup accepted"); }
        catch (JSONException | IllegalArgumentException expected) { }
    }
}
