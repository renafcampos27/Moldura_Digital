package pt.molduratv;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.Assert.*;

public class LocalFoldersTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    @Test public void listsFoldersFirstAndOnlyJsonFilesInBackupMode() throws Exception {
        File root = temporary.newFolder("root");
        File alpha = new File(root, "alpha"), zebra = new File(root, "Zebra");
        assertTrue(alpha.mkdir()); assertTrue(zebra.mkdir());
        File backup = new File(root, "backup.JSON"); assertTrue(backup.createNewFile());
        assertTrue(new File(root, "photo.jpg").createNewFile());
        assertEquals(Arrays.asList(alpha, zebra), LocalFolders.children(root, false));
        assertEquals(Arrays.asList(alpha, zebra, backup), LocalFolders.children(root, true));
    }
    @Test public void scansSubfoldersAndAvoidsDuplicateOverlappingRoots() throws Exception {
        File root = temporary.newFolder("collection"), nested = new File(root, "nested"); assertTrue(nested.mkdir());
        assertTrue(new File(root, "01.JPG").createNewFile());
        assertTrue(new File(nested, "02.MP4").createNewFile());
        assertTrue(new File(nested, "notes.txt").createNewFile());
        List<MediaEntry> entries = new ArrayList<>(); List<String> warnings = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        LocalFolders.scan(root, visited, entries, warnings);
        LocalFolders.scan(nested, visited, entries, warnings);
        assertEquals(2, entries.size()); assertTrue(warnings.isEmpty());
        assertTrue(entries.stream().anyMatch(e -> e.isPhoto() && e.name.equals("01.JPG")));
        assertTrue(entries.stream().anyMatch(e -> !e.isPhoto() && e.name.equals("02.MP4")));
        for (MediaEntry entry : entries) assertTrue(FolderSource.isLocal(entry.uri));
    }
    @Test public void localFolderUrisPreserveSpacesAccentsAndCanonicalPaths() throws Exception {
        File folder = temporary.newFolder("Fotos de férias");
        String reference = FolderSource.fromFile(folder);
        FolderSource.validate(reference, true);
        assertEquals(folder.getCanonicalFile(), FolderSource.localFile(reference));
        assertTrue(FolderSource.display(reference).contains("Fotos de férias"));
        try { FolderSource.validate(reference, false); fail(); } catch (IllegalArgumentException expected) { }
    }
    @Test public void missingFolderProducesAnErrorAndDoesNotInventEntries() throws Exception {
        File missing = new File(temporary.getRoot(), "unplugged-usb");
        try { LocalFolders.children(missing, false); fail(); } catch (IOException expected) { }
        List<MediaEntry> entries = new ArrayList<>();
        try { LocalFolders.scan(missing, new HashSet<>(), entries, new ArrayList<>()); fail(); }
        catch (IOException expected) { }
        assertTrue(entries.isEmpty());
    }
    @Test public void interruptedScanStopsWithoutReadingTheWholeTree() throws Exception {
        File root = temporary.newFolder("cancelled"); assertTrue(new File(root, "image.jpg").createNewFile());
        List<MediaEntry> entries = new ArrayList<>();
        Thread.currentThread().interrupt();
        try { LocalFolders.scan(root, new HashSet<>(), entries, new ArrayList<>()); assertTrue(entries.isEmpty()); }
        finally { Thread.interrupted(); }
    }
}
