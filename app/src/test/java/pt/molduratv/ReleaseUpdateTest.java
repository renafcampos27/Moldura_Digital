package pt.molduratv;

import org.json.*;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class ReleaseUpdateTest {
    static JSONObject release(byte[] payload) throws Exception {
        byte[] hash = java.security.MessageDigest.getInstance("SHA-256").digest(payload);
        StringBuilder digest = new StringBuilder("sha256:");
        for (byte value : hash) digest.append(String.format(Locale.ROOT, "%02x", value & 255));
        JSONObject asset = new JSONObject().put("name", "MolduraTV-0.4.0.apk").put("state", "uploaded")
            .put("browser_download_url", "https://github.com/renafcampos27/Moldura_Digital/releases/download/v0.4.0/MolduraTV-0.4.0.apk")
            .put("size", payload.length).put("digest", digest.toString());
        return new JSONObject().put("tag_name", "v0.4.0").put("draft", false).put("prerelease", false)
            .put("assets", new JSONArray().put(asset));
    }
    @Test public void comparesVersionsNumericallyAndRejectsUnstableTags() {
        assertTrue(ReleaseUpdate.compareVersions("v0.10.0", "0.9.9") > 0);
        assertEquals(0, ReleaseUpdate.compareVersions("v1.2.3", "1.2.3"));
        for (String invalid : Arrays.asList("1.2", "1.2.3-beta", "latest", "1.2.999999999999999", "01.2.3")) {
            try { ReleaseUpdate.normalize(invalid); fail(); } catch (IllegalArgumentException expected) { }
        }
    }
    @Test public void findsNewStableReleaseAndIgnoresEqualOrOlderRelease() throws Exception {
        String json = release(new byte[]{1,2,3}).toString();
        ReleaseUpdate update = ReleaseUpdate.parse(json, "0.3.0");
        assertNotNull(update); assertEquals("0.4.0", update.version); assertEquals(3, update.size);
        assertNull(ReleaseUpdate.parse(json, "0.4.0")); assertNull(ReleaseUpdate.parse(json, "1.0.0"));
    }
    @Test public void rejectsDraftPrereleaseMissingAndAmbiguousApks() throws Exception {
        JSONObject release = release(new byte[]{1}); release.put("draft", true); reject(release);
        release.put("draft", false).put("prerelease", true); reject(release);
        release = release(new byte[]{1});
        release.getJSONArray("assets").put(release.getJSONArray("assets").get(0)); reject(release);
        release.put("assets", new JSONArray()); reject(release);
    }
    @Test public void rejectsWrongRepositoryUntrustedHashAndInvalidSizes() throws Exception {
        JSONObject release = release(new byte[]{1});
        JSONObject asset = release.getJSONArray("assets").getJSONObject(0);
        String original = asset.getString("browser_download_url");
        asset.put("browser_download_url", original.replace("renafcampos27", "other")); reject(release);
        asset.put("browser_download_url", original).put("digest", JSONObject.NULL); reject(release);
        release = release(new byte[]{1}); asset = release.getJSONArray("assets").getJSONObject(0);
        asset.put("size", 0); reject(release);
        asset.put("size", ReleaseUpdate.MAX_APK_BYTES + 1); reject(release);
        asset.put("size", "123"); reject(release);
    }
    @Test public void onlyAllowsHttpsRedirectsOnExplicitGithubHosts() {
        assertTrue(ReleaseUpdate.allowedDownloadUrl("https://release-assets.githubusercontent.com/asset?token=temporary"));
        assertTrue(ReleaseUpdate.allowedDownloadUrl("https://objects.githubusercontent.com/asset"));
        for (String bad : Arrays.asList("http://github.com/file", "https://github.com.evil.test/file", "https://user@github.com/file", "https://example.com/file", "https://github.com:8080/file", "file:///tmp/file"))
            assertFalse(bad, ReleaseUpdate.allowedDownloadUrl(bad));
    }
    private void reject(JSONObject json) throws Exception {
        try { ReleaseUpdate.parse(json.toString(), "0.3.0"); fail("Invalid release accepted"); }
        catch (IllegalArgumentException | JSONException expected) { }
    }
}
