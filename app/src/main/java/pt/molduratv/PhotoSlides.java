package pt.molduratv;

import java.util.*;
import java.util.function.ToDoubleFunction;

/** Ordered slides and fit-only geometry. Independent of Android for regression tests. */
public final class PhotoSlides {
    public static final int AUTOMATIC = 0, INDIVIDUAL = 1, PAIRS = 2;
    public static String modeName(int mode) {
        return new String[]{"Automático", "Individual", "Dois lado a lado"}[mode];
    }
    public static final class Slide {
        public final MediaEntry first, second;
        public final double firstRatio, secondRatio;
        public Slide(MediaEntry first, double ratio) { this(first, ratio, null, 0); }
        public Slide(MediaEntry first, double firstRatio, MediaEntry second, double secondRatio) {
            this.first = first; this.firstRatio = firstRatio;
            this.second = second; this.secondRatio = secondRatio;
        }
        public boolean paired() { return second != null; }
    }
    private PhotoSlides() {}
    public static List<Slide> arrange(List<MediaEntry> entries, boolean smart, double screenRatio,
                                      ToDoubleFunction<MediaEntry> ratio) {
        return arrange(entries, smart ? AUTOMATIC : INDIVIDUAL, screenRatio, false, ratio);
    }
    public static List<Slide> arrange(List<MediaEntry> entries, int mode, double screenRatio,
                                      boolean matchSimilar, ToDoubleFunction<MediaEntry> ratio) {
        if (mode < AUTOMATIC || mode > PAIRS) throw new IllegalArgumentException("Invalid presentation mode");
        boolean[] paired = new boolean[entries.size()];
        List<Slide> slides = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            if (paired[i]) continue;
            MediaEntry first = entries.get(i);
            double a = mode != INDIVIDUAL ? ratio.applyAsDouble(first) : 0;
            int best = -1; double bestRatio = 0, score = Double.POSITIVE_INFINITY;
            if (mode != INDIVIDUAL && eligible(first, a, screenRatio)) {
                int checked = 0, limit = matchSimilar ? 12 : 1;
                for (int j = i + 1; j < entries.size() && checked < limit; j++) {
                    if (paired[j]) continue;
                    checked++;
                    MediaEntry candidate = entries.get(j);
                    if (candidate.isPhoto() != first.isPhoto()) break;
                    double b = ratio.applyAsDouble(candidate);
                    if (!eligible(candidate, b, screenRatio) || (mode == AUTOMATIC && !comfortable(a, b, screenRatio))) continue;
                    double difference = Math.abs(Math.log(a / b));
                    if (difference < score) { best = j; bestRatio = b; score = difference; }
                }
            }
            if (best >= 0) {
                paired[best] = true;
                MediaEntry second = entries.get(best);
                slides.add(new Slide(first, a, second, bestRatio)); continue;
            }
            slides.add(new Slide(first, a));
        }
        return slides;
    }
    public static boolean comfortable(double left, double right, double screenRatio) {
        double height = Math.min(1, screenRatio / (left + right));
        return height >= 0.55 && height * Math.min(left, right) / screenRatio >= 0.12;
    }
    /** Dimensions reported by Media3 already include applied rotation; pixel aspect still matters. */
    public static double displayedRatio(int width, int height, double pixelRatio) {
        if (width <= 0 || height <= 0 || !Double.isFinite(pixelRatio) || pixelRatio <= 0) return 0;
        return width * pixelRatio / height;
    }
    private static boolean eligible(MediaEntry entry, double ratio, double screenRatio) {
        // A 16:9 landscape photo stays individual even on an ultrawide display.
        return narrow(ratio, screenRatio) && (entry.isPhoto() ? ratio < (16d / 9) * 0.95 : ratio < 1);
    }
    private static boolean narrow(double ratio, double screenRatio) {
        return Double.isFinite(ratio) && ratio > 0 && ratio < screenRatio * 0.95;
    }
    /** x, y, left width, right width, height. Common height leaves no internal gutter. */
    public static int[] fit(int width, int height, double left, double right) {
        if (width < 2 || height <= 0 || !Double.isFinite(left + right) || left <= 0 || right <= 0)
            throw new IllegalArgumentException("Invalid photo geometry");
        double h = Math.min(height, width / (left + right));
        int total = Math.min(width, Math.max(2, (int)Math.round(h * (left + right))));
        int a = Math.max(1, Math.min(total - 1, (int)Math.round(h * left)));
        int b = total - a;
        int pixels = Math.max(1, (int) Math.round(h));
        return new int[]{(width - a - b) / 2, (height - pixels) / 2, a, b, pixels};
    }
}
