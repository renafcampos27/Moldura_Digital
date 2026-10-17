package pt.molduratv;

import java.io.StringReader;
import java.util.*;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

/** Conservative embedded Motion Photo recognition. Never guesses from a name or a neighbouring MOV. */
public final class MotionPhotos {
    public static final int PHOTO = 0, VIDEO = 1, PHOTO_THEN_VIDEO = 2;
    public static String modeName(int mode) { return new String[]{"Fotografia","Movimento","Fotografia e movimento"}[mode]; }
    private MotionPhotos() {}
    public static boolean hasEmbeddedVideo(String xmp, long totalBytes) {
        if (xmp == null || xmp.length() > 262144 || totalBytes <= 0 || xmp.toUpperCase(Locale.ROOT).contains("<!DOCTYPE")) return false;
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance(); factory.setNamespaceAware(true);
            XMLReader reader = factory.newSAXParser().getXMLReader();
            reader.setFeature("http://xml.org/sax/features/external-general-entities",false);
            reader.setFeature("http://xml.org/sax/features/external-parameter-entities",false);
            reader.setEntityResolver((publicId,systemId) -> new InputSource(new StringReader("")));
            List<long[]> items = new ArrayList<>(); boolean[] enabled = {false}; long[] oldOffset = {0};
            reader.setContentHandler(new DefaultHandler() {
                @Override public void startElement(String uri,String name,String qualified,Attributes attrs) {
                    String mime = "", semantic = ""; long length = 0, padding = 0;
                    for (int i = 0; i < attrs.getLength(); i++) {
                        String namespace = attrs.getURI(i), key = attrs.getLocalName(i), value = attrs.getValue(i);
                        if ("http://ns.google.com/photos/1.0/camera/".equals(namespace)) {
                            if (("MotionPhoto".equals(key) || "MicroVideo".equals(key)) && "1".equals(value)) enabled[0] = true;
                            if ("MicroVideoOffset".equals(key)) oldOffset[0] = number(value);
                        }
                        if ("http://ns.google.com/photos/1.0/container/item/".equals(namespace)) {
                            if ("Mime".equals(key)) mime = value;
                            if ("Semantic".equals(key)) semantic = value;
                            if ("Length".equals(key)) length = number(value);
                            if ("Padding".equals(key)) padding = number(value);
                        }
                    }
                    if (!mime.isEmpty()) items.add(new long[]{length,padding,"video/mp4".equals(mime) && "MotionPhoto".equals(semantic) ? 1 : 0});
                }
            });
            reader.parse(new InputSource(new StringReader(xmp)));
            if (!enabled[0]) return false;
            if (oldOffset[0] > 0 && oldOffset[0] < totalBytes) return true;
            long position = totalBytes;
            for (int i = items.size()-1; i > 0; i--) {
                long[] item = items.get(i);
                if (item[0] < 0 || item[1] < 0 || item[0] > position || item[1] > position - item[0]) return false;
                position -= item[0] + item[1];
                if (item[2] == 1 && item[0] > 0 && position > 0) return true;
            }
        } catch (Exception ignored) { }
        return false;
    }
    private static long number(String value) { try { return Long.parseLong(value); } catch (NumberFormatException error) { return -1; } }
}
