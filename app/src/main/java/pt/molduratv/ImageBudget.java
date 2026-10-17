package pt.molduratv;

/** Conservative pixel budget; preserves originals and caps each decoder, including wide-colour output. */
public final class ImageBudget {
    private ImageBudget() {}
    public static int maxEdge(int width, int height, long heapBytes, boolean paired, boolean process64Bit) {
        int display = Math.max(1, Math.max(width,height));
        int samplingLimit = (int)Math.min(Integer.MAX_VALUE,2L*display-1);
        long bytes = Math.min(64L*1024*1024, Math.max(2L*1024*1024, heapBytes / (paired ? 8 : 5)));
        int memoryEdge = Math.max(1,(int)Math.sqrt(bytes / 8d));
        int architectureEdge = process64Bit ? 4096 : 2048;
        return Math.max(1,Math.min(samplingLimit,Math.min(memoryEdge,architectureEdge)));
    }
}
