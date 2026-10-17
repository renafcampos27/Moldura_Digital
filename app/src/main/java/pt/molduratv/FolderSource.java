package pt.molduratv;

import java.io.*;
import java.net.URI;

/** A saved folder is either a SAF tree or an absolute local directory. */
public final class FolderSource {
    private FolderSource() {}
    public static boolean isLocal(String value) { return value != null && value.startsWith("file:"); }
    public static void validate(String value, boolean allowLocal) {
        try {
            URI uri = new URI(value); String path = uri.getRawPath();
            if (value.length() > 8192 || uri.getRawQuery() != null || uri.getRawFragment() != null) throw new IllegalArgumentException();
            if ("content".equals(uri.getScheme()) && uri.getRawAuthority() != null && !uri.getRawAuthority().isEmpty()
                && path != null && path.startsWith("/tree/") && path.length() > 6 && !path.substring(6).contains("/")) return;
            if (allowLocal && "file".equals(uri.getScheme()) && (uri.getRawAuthority() == null || uri.getRawAuthority().isEmpty())
                && uri.getPath() != null && uri.getPath().startsWith("/") && !uri.getPath().contains("\u0000")) {
                for (String segment : uri.getPath().split("/")) if (segment.equals(".") || segment.equals("..")) throw new IllegalArgumentException();
                return;
            }
            throw new IllegalArgumentException();
        } catch (Exception error) { throw new IllegalArgumentException("Referência de pasta inválida.", error); }
    }
    public static File localFile(String value) throws IOException {
        validate(value, true);
        if (!isLocal(value)) throw new IOException("A pasta não é local.");
        return new File(URI.create(value)).getCanonicalFile();
    }
    public static String fromFile(File file) throws IOException { return file.getCanonicalFile().toURI().toASCIIString(); }
    public static String display(String value) {
        URI uri = URI.create(value);
        return isLocal(value) ? uri.getPath() : uri.getPath().substring(6) + " · " + uri.getAuthority();
    }
}
