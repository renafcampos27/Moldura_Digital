package pt.molduratv;

import org.json.*;
import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

/** Versioned configuration only. Android access grants cannot be exported. */
public final class SettingsBackup {
    public static final int MAX_BYTES = 1_048_576, MAX_FOLDERS = 512;
    private static final String FORMAT = "molduratv-settings";

    public static final class Data {
        public final List<String> roots;
        public final int seconds, order, layoutMode, motionMode, liveMode;
        public final PlaybackFilter filter;
        public final boolean crop, muted, loop, smartPhotos;
        public Data(List<String> roots, int seconds, int order, boolean crop, boolean muted, boolean loop) {
            this(roots, seconds, order, crop, muted, loop, true);
        }
        public Data(List<String> roots, int seconds, int order, boolean crop, boolean muted, boolean loop, boolean smartPhotos) {
            this(roots, seconds, order, crop, muted, loop, smartPhotos, smartPhotos ? PhotoSlides.AUTOMATIC : PhotoSlides.INDIVIDUAL);
        }
        public Data(List<String> roots, int seconds, int order, boolean crop, boolean muted, boolean loop, boolean smartPhotos, int layoutMode) {
            this(roots,seconds,order,crop,muted,loop,smartPhotos,layoutMode,MotionPhotos.PHOTO);
        }
        public Data(List<String> roots, int seconds, int order, boolean crop, boolean muted, boolean loop, boolean smartPhotos, int layoutMode,int motionMode) {
            this(roots,seconds,order,crop,muted,loop,smartPhotos,layoutMode,motionMode,PlaybackFilter.all());
        }
        public Data(List<String> roots, int seconds, int order, boolean crop, boolean muted, boolean loop, boolean smartPhotos, int layoutMode,int motionMode,PlaybackFilter filter) {
            this(roots,seconds,order,crop,muted,loop,smartPhotos,layoutMode,motionMode,filter,LivePhotos.BOTH);
        }
        public Data(List<String> roots, int seconds, int order, boolean crop, boolean muted, boolean loop, boolean smartPhotos, int layoutMode,int motionMode,PlaybackFilter filter,int liveMode) {
            if (roots == null || roots.size() > MAX_FOLDERS) throw new IllegalArgumentException("Número de pastas inválido.");
            for (String root : roots) FolderSource.validate(root, true);
            if (!Arrays.asList(5, 10, 30, 60, 120, 300).contains(seconds) || order < 0 || order > 3 || liveMode<0 || liveMode>3
                    || layoutMode < 0 || layoutMode > 2 || motionMode < 0 || motionMode > 2 || smartPhotos != (layoutMode != PhotoSlides.INDIVIDUAL))
                throw new IllegalArgumentException("Opções da apresentação inválidas.");
            this.roots = Collections.unmodifiableList(new ArrayList<>(new LinkedHashSet<>(roots)));
            this.seconds = seconds; this.order = order; this.crop = crop; this.muted = muted; this.loop = loop;
            this.smartPhotos = smartPhotos;
            this.layoutMode = layoutMode;
            this.motionMode=motionMode;
            this.filter=Objects.requireNonNull(filter);
            this.liveMode=liveMode;
        }
    }
    private SettingsBackup() {}
    public static String encode(Data data) throws JSONException {
        JSONObject options = new JSONObject().put("photoSeconds", data.seconds).put("order", data.order)
            .put("crop", data.crop).put("muted", data.muted).put("loop", data.loop).put("smartPhotos", data.smartPhotos)
            .put("presentationMode", data.layoutMode).put("motionPhotoMode",data.motionMode).put("playbackFilter",data.filter.json()).put("livePhotoMode",data.liveMode);
        String encoded = new JSONObject().put("format", FORMAT).put("schemaVersion", 7)
            .put("createdAt", Instant.now().toString()).put("folders", new JSONArray(data.roots))
            .put("options", options).toString(2) + "\n";
        if (encoded.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES)
            throw new IllegalArgumentException("A configuração excede o tamanho máximo do backup.");
        return encoded;
    }
    /** Local backups are new files; an existing backup is never truncated. */
    public static void writeNew(File target, Data data) throws IOException, JSONException {
        byte[] payload = encode(data).getBytes(StandardCharsets.UTF_8);
        try (OutputStream output = java.nio.file.Files.newOutputStream(target.toPath(),
                java.nio.file.StandardOpenOption.CREATE_NEW, java.nio.file.StandardOpenOption.WRITE)) {
            output.write(payload); output.flush();
        }
    }
    public static Data read(InputStream input) throws IOException, JSONException {
        if (input == null) throw new IOException("Não foi possível abrir o ficheiro.");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192]; int count;
        while ((count = input.read(buffer)) != -1) {
            if (output.size() + count > MAX_BYTES) throw new IOException("O ficheiro de backup é demasiado grande.");
            output.write(buffer, 0, count);
        }
        return decode(new String(output.toByteArray(), StandardCharsets.UTF_8));
    }
    public static Data decode(String text) throws JSONException {
        if (text == null || text.length() > MAX_BYTES) throw new IllegalArgumentException("Backup inválido.");
        checkNesting(text);
        JSONTokener tokens = new JSONTokener(text);
        Object parsed = tokens.nextValue();
        if (!(parsed instanceof JSONObject) || tokens.nextClean() != 0) throw new IllegalArgumentException("Backup inválido.");
        JSONObject document = (JSONObject) parsed;
        int schema = integer(document, "schemaVersion");
        if (!FORMAT.equals(document.get("format")) || (schema < 1 || schema > 7))
            throw new IllegalArgumentException("O ficheiro não é um backup compatível da MolduraTV.");
        JSONArray folders = document.getJSONArray("folders");
        if (folders.length() > MAX_FOLDERS) throw new IllegalArgumentException("O backup tem demasiadas pastas.");
        List<String> roots = new ArrayList<>();
        for (int i = 0; i < folders.length(); i++) {
            if (!(folders.get(i) instanceof String)) throw new IllegalArgumentException("Pasta inválida.");
            String root = (String) folders.get(i);
            FolderSource.validate(root, schema >= 2); roots.add(root);
        }
        JSONObject options = document.getJSONObject("options");
        boolean smart = schema >= 3 ? bool(options, "smartPhotos") : true;
        return new Data(roots, integer(options, "photoSeconds"), integer(options, "order"),
            bool(options, "crop"), bool(options, "muted"), bool(options, "loop"),
            smart, schema >= 4 ? integer(options, "presentationMode") : smart ? PhotoSlides.AUTOMATIC : PhotoSlides.INDIVIDUAL,
            schema >= 5 ? integer(options,"motionPhotoMode") : MotionPhotos.PHOTO,
            schema >= 6 ? (schema<7?PlaybackFilter.read(options.getJSONObject("playbackFilter")).upgradeFormats():PlaybackFilter.read(options.getJSONObject("playbackFilter"))) : PlaybackFilter.all(),
            schema >= 7 ? integer(options,"livePhotoMode") : LivePhotos.SEPARATE);
    }
    private static void checkNesting(String text) {
        int depth = 0; boolean quoted = false, escaped = false;
        for (int i = 0; i < text.length(); i++) {
            char value = text.charAt(i);
            if (quoted) {
                if (escaped) escaped = false;
                else if (value == '\\') escaped = true;
                else if (value == '"') quoted = false;
            } else if (value == '"') quoted = true;
            else if (value == '\'' || value == '/') throw new IllegalArgumentException("Usa um ficheiro JSON sem comentários.");
            else if (value == '{' || value == '[') {
                if (++depth > 32) throw new IllegalArgumentException("Estrutura de backup demasiado complexa.");
            } else if (value == '}' || value == ']') depth--;
        }
    }
    private static int integer(JSONObject object, String key) throws JSONException {
        Object value = object.get(key);
        if (!(value instanceof Integer || value instanceof Long)) throw new IllegalArgumentException("Número inválido: " + key);
        long number = ((Number) value).longValue();
        if (number < Integer.MIN_VALUE || number > Integer.MAX_VALUE) throw new IllegalArgumentException("Número inválido: " + key);
        return (int) number;
    }
    private static boolean bool(JSONObject object, String key) throws JSONException {
        Object value = object.get(key);
        if (!(value instanceof Boolean)) throw new IllegalArgumentException("Opção inválida: " + key);
        return (Boolean) value;
    }
}
