package pt.molduratv;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/** Local filesystem operations kept outside Android UI for testing and background use. */
public final class LocalFolders {
    private LocalFolders() {}
    public static List<File> children(File directory, boolean jsonFiles) throws IOException {
        File[] files = directory.listFiles();
        if (files == null) throw new IOException("Não foi possível ler esta pasta. Verifica as permissões e a ligação do disco.");
        List<File> result = new ArrayList<>();
        for (File file : files) {
            if (Files.isSymbolicLink(file.toPath())) continue;
            if (file.isDirectory() || (jsonFiles && file.isFile() && file.getName().toLowerCase(Locale.ROOT).endsWith(".json"))) result.add(file);
        }
        result.sort(Comparator.comparing((File f) -> !f.isDirectory()).thenComparing(File::getName, String.CASE_INSENSITIVE_ORDER));
        return result;
    }
    public static void scan(File root, Set<String> visited, List<MediaEntry> entries, List<String> warnings) throws IOException {
        Path canonical = root.getCanonicalFile().toPath();
        if (!Files.isDirectory(canonical)) throw new IOException("A pasta deixou de estar disponível: " + root.getPath());
        Files.walkFileTree(canonical, new SimpleFileVisitor<Path>() {
            @Override public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes) throws IOException {
                if (Thread.currentThread().isInterrupted()) return FileVisitResult.TERMINATE;
                return visited.add(directory.toRealPath().toString()) ? FileVisitResult.CONTINUE : FileVisitResult.SKIP_SUBTREE;
            }
            @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                if (Thread.currentThread().isInterrupted()) return FileVisitResult.TERMINATE;
                if (attributes.isRegularFile()) {
                    String mime = MediaEntry.supportedMime(file.getFileName().toString(), null);
                    if (mime != null) entries.add(new MediaEntry(FolderSource.fromFile(file.toFile()), file.getFileName().toString(), mime, attributes.lastModifiedTime().toMillis(), attributes.size()));
                }
                return FileVisitResult.CONTINUE;
            }
            @Override public FileVisitResult visitFileFailed(Path file, IOException error) {
                if (warnings.size() < 20) warnings.add("Não foi possível ler: " + file.getFileName());
                return Thread.currentThread().isInterrupted() ? FileVisitResult.TERMINATE : FileVisitResult.CONTINUE;
            }
        });
    }
}
