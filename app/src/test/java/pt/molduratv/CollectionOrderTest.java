package pt.molduratv;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class CollectionOrderTest {
    private MediaEntry entry(String uri, String name, long date) { return new MediaEntry(uri, name, "image/jpeg", date); }
    @Test public void overlappingFoldersDoNotRepeatTheSameUri() {
        MediaEntry first = entry("a", "Z.jpg", 1), second = entry("b", "a.jpg", 2);
        List<MediaEntry> output = CollectionOrder.arrange(List.of(first, second, first), 0, new Random(1));
        assertEquals(List.of(second, first), output);
    }
    @Test public void dateOrderUsesOldestFirstAndNameToBreakTies() {
        MediaEntry a = entry("a", "a.jpg", 2), b = entry("b", "b.jpg", 2), c = entry("c", "z.jpg", 1);
        assertEquals(List.of(c, a, b), CollectionOrder.arrange(List.of(b, c, a), 1, new Random(1)));
    }
    @Test public void randomOrderPreservesTheCollectionAndDoesNotMutateInput() {
        List<MediaEntry> input = List.of(entry("a", "A", 1), entry("b", "B", 2), entry("c", "C", 3));
        List<MediaEntry> output = CollectionOrder.arrange(input, 2, new Random(5));
        assertEquals(new HashSet<>(input), new HashSet<>(output));
        assertEquals("a", input.get(0).uri);
    }
    @Test public void recognizesHeicAndVideoWithoutTrustingGenericProviderMime() {
        assertEquals("image/heic", MediaEntry.supportedMime("PHOTO.HEIC", "application/octet-stream"));
        assertEquals("video/mp4", MediaEntry.supportedMime("clip.MP4", null));
        assertNull(MediaEntry.supportedMime("notes.txt", "text/plain"));
        assertNull(MediaEntry.supportedMime("animation.gif", "image/gif"));
    }
}
