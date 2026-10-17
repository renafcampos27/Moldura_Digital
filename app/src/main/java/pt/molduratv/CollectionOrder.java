package pt.molduratv;

import java.util.*;

public final class CollectionOrder {
    private CollectionOrder() {}
    public static List<MediaEntry> arrange(List<MediaEntry> input, int order, Random random) {
        Map<String, MediaEntry> unique = new LinkedHashMap<>();
        for (MediaEntry entry : input) unique.putIfAbsent(entry.uri, entry);
        List<MediaEntry> result = new ArrayList<>(unique.values());
        Comparator<MediaEntry> names = Comparator.comparing(e -> e.name, String.CASE_INSENSITIVE_ORDER);
        result.sort(order == 1 ? Comparator.comparingLong((MediaEntry e) -> e.modified).thenComparing(names) : names);
        if (order == 2) Collections.shuffle(result, random);
        return result;
    }
}
