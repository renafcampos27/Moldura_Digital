package pt.molduratv;

import org.json.*;
import java.net.URI;
import java.util.*;
import java.util.regex.*;

/** Strict contract for the one public repository used by the app. No access tokens. */
public final class ReleaseUpdate {
    public static final String REPOSITORY = "renafcampos27/Moldura_Digital";
    public static final String RELEASES_URL = "https://github.com/" + REPOSITORY + "/releases";
    public static final String API_URL = "https://api.github.com/repos/" + REPOSITORY + "/releases/latest";
    public static final long MAX_APK_BYTES = 256L * 1024 * 1024;
    private static final Pattern VERSION = Pattern.compile("v?(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)");
    public final String version, name, url, sha256;
    public final long size;
    private ReleaseUpdate(String version, String name, String url, String sha256, long size) {
        this.version = version; this.name = name; this.url = url; this.sha256 = sha256; this.size = size;
    }
    public static String normalize(String value) {
        if (value == null || value.length() > 40 || !VERSION.matcher(value).matches())
            throw new IllegalArgumentException("A versão deve ter o formato v1.2.3.");
        String result = value.startsWith("v") ? value.substring(1) : value;
        for (String part : result.split("\\.")) Integer.parseInt(part);
        return result;
    }
    public static int compareVersions(String first, String second) {
        String[] a = normalize(first).split("\\."), b = normalize(second).split("\\.");
        for (int i = 0; i < 3; i++) {
            int comparison = Integer.compare(Integer.parseInt(a[i]), Integer.parseInt(b[i]));
            if (comparison != 0) return comparison;
        }
        return 0;
    }
    /** Returns null for an already installed/older stable release. */
    public static ReleaseUpdate parse(String json, String installed) throws JSONException {
        JSONObject release = new JSONObject(json);
        if (release.getBoolean("draft") || release.getBoolean("prerelease"))
            throw new IllegalArgumentException("A publicação ainda não é uma versão estável.");
        String tag = release.getString("tag_name"), version = normalize(tag);
        if (compareVersions(version, installed) <= 0) return null;
        Set<String> names = new HashSet<>(Arrays.asList("MolduraTV-" + version + ".apk", "MolduraTV-v" + version + ".apk", "app-release.apk"));
        JSONArray assets = release.getJSONArray("assets"); JSONObject match = null;
        for (int i = 0; i < assets.length(); i++) {
            JSONObject asset = assets.getJSONObject(i);
            if (!names.contains(asset.getString("name")) || !"uploaded".equals(asset.optString("state"))) continue;
            if (match != null) throw new IllegalArgumentException("A Release tem mais de um APK elegível. Publica apenas um APK universal.");
            match = asset;
        }
        if (match == null) throw new IllegalArgumentException("A nova Release ainda não tem um APK universal compatível anexado.");
        String name = match.getString("name"), url = match.getString("browser_download_url");
        String expected = "https://github.com/" + REPOSITORY + "/releases/download/" + tag + "/" + name;
        if (!expected.equals(url)) throw new IllegalArgumentException("O endereço do APK não pertence à Release esperada.");
        Object rawSize = match.get("size");
        if (!(rawSize instanceof Integer || rawSize instanceof Long)) throw new IllegalArgumentException("Tamanho de atualização inválido.");
        long size = ((Number)rawSize).longValue();
        if (size <= 0 || size > MAX_APK_BYTES) throw new IllegalArgumentException("Tamanho de atualização inválido.");
        String digest = match.optString("digest", "");
        if (!digest.matches("sha256:[0-9a-fA-F]{64}"))
            throw new IllegalArgumentException("O GitHub não indicou o SHA-256 deste APK. Volta a anexá-lo à Release.");
        return new ReleaseUpdate(version, name, url, digest.substring(7).toLowerCase(Locale.ROOT), size);
    }
    public static boolean allowedDownloadUrl(String value) {
        try {
            URI uri = new URI(value);
            return "https".equals(uri.getScheme()) && uri.getRawUserInfo() == null
                && (uri.getPort() == -1 || uri.getPort() == 443)
                && Arrays.asList("github.com", "release-assets.githubusercontent.com", "objects.githubusercontent.com", "github-releases.githubusercontent.com")
                    .contains(uri.getHost());
        } catch (Exception error) { return false; }
    }
}
