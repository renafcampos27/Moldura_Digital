package pt.molduratv;
import org.junit.Test;
import static org.junit.Assert.*;
public class ImageBudgetTest {
    @Test public void allowsBoundedOversamplingForPowerOfTwoDecodingWithoutUnlimitedResolution() {
        assertEquals(1439,ImageBudget.maxEdge(720,480,512L<<20,false,true));
        assertEquals(1,ImageBudget.maxEdge(0,0,128L<<20,true,false));
    }
    @Test public void pairedDecodersAnd32BitHaveConservativeLimits() {
        int single=ImageBudget.maxEdge(3840,2160,256L<<20,false,true);
        int pair=ImageBudget.maxEdge(3840,2160,256L<<20,true,true);
        assertTrue(pair<=single); assertTrue(ImageBudget.maxEdge(3840,2160,512L<<20,false,false)<=2048);
        assertTrue(ImageBudget.maxEdge(3840,2160,64L<<20,true,false)<pair);
    }
    @Test public void pixelBudgetAllowsEightBytesPerPixelWithoutExceedingTheShare() {
        for (long heap : new long[]{64L<<20,128L<<20,256L<<20}) {
            int edge=ImageBudget.maxEdge(3840,2160,heap,true,true);
            assertTrue((long)edge*edge*8<=heap/8);
        }
    }
}
