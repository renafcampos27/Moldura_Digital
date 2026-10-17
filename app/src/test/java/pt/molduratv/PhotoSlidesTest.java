package pt.molduratv;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class PhotoSlidesTest {
    private MediaEntry photo(String name) { return new MediaEntry("file:///" + name, name, "image/jpeg", 0); }
    private MediaEntry video() { return new MediaEntry("file:///video.mp4", "video", "video/mp4", 0); }
    @Test public void pairsPortraitAndFourByThreeWithoutReorderingOrLosingOddPhoto() {
        List<MediaEntry> photos = Arrays.asList(photo("a"), photo("b"), photo("c"), photo("d"), photo("e"));
        List<PhotoSlides.Slide> slides = PhotoSlides.arrange(photos, true, 16d / 9, e -> e.name.equals("a") ? 9d / 16 : 4d / 3);
        assertEquals(3, slides.size()); assertSame(photos.get(0), slides.get(0).first);
        assertSame(photos.get(1), slides.get(0).second); assertSame(photos.get(2), slides.get(1).first);
        assertSame(photos.get(3), slides.get(1).second); assertSame(photos.get(4), slides.get(2).first);
        assertFalse(slides.get(2).paired());
    }
    @Test public void videosAndWidePhotosRemainSingleAndPairingNeverCrossesThem() {
        List<MediaEntry> photos = Arrays.asList(photo("a"), video(), photo("wide"), photo("b"), photo("c"));
        List<PhotoSlides.Slide> slides = PhotoSlides.arrange(photos, true, 16d / 9, e -> e.name.equals("wide") ? 16d / 9 : .75);
        assertEquals(4, slides.size());
        for (int i = 0; i < 3; i++) { assertSame(photos.get(i), slides.get(i).first); assertFalse(slides.get(i).paired()); }
        assertSame(photos.get(3), slides.get(3).first); assertSame(photos.get(4), slides.get(3).second);
    }
    @Test public void unknownOrInvalidDimensionsStaySingle() {
        List<MediaEntry> photos = Arrays.asList(photo("a"), photo("b"));
        for (double invalid : new double[]{0, -1, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertEquals(2, PhotoSlides.arrange(photos, true, 16d / 9, e -> invalid).size());
            assertEquals(2, PhotoSlides.arrange(photos, true, 16d / 9, e -> e.name.equals("a") ? .75 : invalid).size());
        }
    }
    @Test public void disabledModeDoesNotReadHeadersOrPairPhotos() {
        assertEquals(2, PhotoSlides.arrange(Arrays.asList(photo("a"), photo("b")), false, 16d / 9,
            e -> { throw new AssertionError("Unexpected header read"); }).size());
    }
    @Test public void randomMatchingFindsClosestFormatWithoutMutatingTheOriginalOrder() {
        MediaEntry a = photo("a"), b = photo("b"), c = photo("c"), d = photo("d");
        List<MediaEntry> input = Arrays.asList(a, b, c, d);
        java.util.function.ToDoubleFunction<MediaEntry> ratios = e -> e == a || e == c ? 9d / 21 : 4d / 3;
        List<PhotoSlides.Slide> random = PhotoSlides.arrange(input, PhotoSlides.AUTOMATIC, 16d / 9, true, ratios);
        assertSame(a, random.get(0).first); assertSame(c, random.get(0).second);
        assertSame(b, random.get(1).first); assertSame(d, random.get(1).second);
        assertEquals(Arrays.asList(a,b,c,d), input);
        List<PhotoSlides.Slide> ordered = PhotoSlides.arrange(input, PhotoSlides.AUTOMATIC, 16d / 9, false, ratios);
        assertSame(b, ordered.get(0).second); assertSame(c, ordered.get(1).first);
    }
    @Test public void matchingDoesNotJumpAcrossAVideoBoundaryOrDropEntries() {
        MediaEntry a = photo("a"), b = photo("b"), movie = video(), c = photo("c");
        List<MediaEntry> input = Arrays.asList(a,b,movie,c);
        List<PhotoSlides.Slide> slides = PhotoSlides.arrange(input, PhotoSlides.AUTOMATIC, 16d / 9, true,
            e -> e == b ? 4d / 3 : 9d / 21);
        assertSame(b, slides.get(0).second); assertSame(movie, slides.get(1).first);
        assertSame(c, slides.get(2).first); assertFalse(slides.get(2).paired());
    }
    @Test public void automaticRejectsTinyPanesButForcedPairsRetainPortraitEligibility() {
        List<MediaEntry> entries = Arrays.asList(video(), video());
        assertEquals(2, PhotoSlides.arrange(entries, PhotoSlides.AUTOMATIC, 16d / 9, false, e -> .15).size());
        assertEquals(1, PhotoSlides.arrange(entries, PhotoSlides.PAIRS, 16d / 9, false, e -> .15).size());
        assertEquals(2, PhotoSlides.arrange(entries, PhotoSlides.PAIRS, 16d / 9, false, e -> 16d / 9).size());
        assertEquals(2, PhotoSlides.arrange(Arrays.asList(photo("a"),photo("b")), PhotoSlides.PAIRS, 21d / 9, true, e -> 16d / 9).size());
    }
    @Test public void modernPortraitFormatsAndMixedRatiosRemainEntireInsideOddSizedScreens() {
        for (double left : new double[]{9d/16, 9d/20, 9d/21, 3d/4}) {
            for (double right : new double[]{9d/16, 9d/20, 9d/21, 3d/4}) {
                assertTrue(PhotoSlides.comfortable(left,right,16d/9));
                int[] fit = PhotoSlides.fit(1919,1079,left,right);
                assertTrue(fit[0] >= 0); assertTrue(fit[1] >= 0);
                assertTrue(fit[0]+fit[2]+fit[3] <= 1919); assertTrue(fit[1]+fit[4] <= 1079);
                assertEquals(left,(double)fit[2]/fit[4],.003); assertEquals(right,(double)fit[3]/fit[4],.003);
            }
        }
        assertEquals(.75,PhotoSlides.displayedRatio(540,720,1),0);
        assertEquals(1.5,PhotoSlides.displayedRatio(540,720,2),0);
        assertEquals(0,PhotoSlides.displayedRatio(0,720,1),0);
        assertEquals(0,PhotoSlides.displayedRatio(540,720,Double.NaN),0);
    }
    @Test public void pairsOnlyConsecutivePortraitVideosAndKeepsOddVideo() {
        List<MediaEntry> videos = Arrays.asList(video(), video(), video());
        List<PhotoSlides.Slide> slides = PhotoSlides.arrange(videos, true, 16d / 9, e -> 9d / 16);
        assertEquals(2, slides.size()); assertTrue(slides.get(0).paired()); assertFalse(slides.get(1).paired());
        assertSame(videos.get(2), slides.get(1).first);
    }
    @Test public void landscapeSquareAndUnknownVideosStaySingle() {
        for (double ratio : new double[]{16d / 9, 4d / 3, 1, 0, Double.NaN}) {
            assertEquals(2, PhotoSlides.arrange(Arrays.asList(video(), video()), true, 16d / 9, e -> ratio).size());
        }
    }
    @Test public void sixteenByNinePhotosAlwaysStaySingleIncludingOnUltrawideScreens() {
        List<MediaEntry> photos = Arrays.asList(photo("wide-a"), photo("wide-b"));
        for (double screen : new double[]{16d / 9, 21d / 9, 32d / 9}) {
            List<PhotoSlides.Slide> slides = PhotoSlides.arrange(photos, true, screen, e -> 16d / 9);
            assertEquals(2, slides.size());
            assertFalse(slides.get(0).paired()); assertFalse(slides.get(1).paired());
        }
    }
    @Test public void widePhotoBetweenPortraitsRemainsIndividualAndKeepsTheOrder() {
        List<MediaEntry> entries = Arrays.asList(photo("a"), photo("wide"), photo("b"), photo("c"));
        List<PhotoSlides.Slide> slides = PhotoSlides.arrange(entries, true, 21d / 9,
            e -> e.name.equals("wide") ? 16d / 9 : 9d / 16);
        assertEquals(3, slides.size()); assertSame(entries.get(0), slides.get(0).first);
        assertFalse(slides.get(0).paired()); assertSame(entries.get(1), slides.get(1).first);
        assertFalse(slides.get(1).paired()); assertSame(entries.get(2), slides.get(2).first);
        assertSame(entries.get(3), slides.get(2).second);
    }
    @Test public void photoVideoBoundariesArePreservedAndMixedPairsAreNeverCreated() {
        MediaEntry a = photo("a"), b = photo("b"), c = video(), d = video();
        List<PhotoSlides.Slide> slides = PhotoSlides.arrange(Arrays.asList(a, c, d, b), true, 16d / 9, e -> .5625);
        assertEquals(3, slides.size()); assertSame(a, slides.get(0).first); assertFalse(slides.get(0).paired());
        assertSame(c, slides.get(1).first); assertSame(d, slides.get(1).second); assertSame(b, slides.get(2).first);
    }
    @Test public void portraitPairTouchesAndKeepsBothWhole() {
        int[] fit = PhotoSlides.fit(1920, 1080, 9d / 16, 9d / 16);
        assertEquals(1080, fit[4]); assertEquals(608, fit[2]); assertTrue(Math.abs(fit[2] - fit[3]) <= 1);
        assertEquals(352, fit[0]); assertEquals(0, fit[1]);
    }
    @Test public void landscapeAndMixedPairsFitInsideScreenAtCommonHeight() {
        for (double left : new double[]{.5625, .75, 1, 4d / 3}) {
            for (double right : new double[]{.5625, .75, 1, 4d / 3}) {
                int[] fit = PhotoSlides.fit(1920, 1080, left, right);
                assertTrue(fit[0] >= 0); assertTrue(fit[1] >= 0);
                assertTrue(fit[0] + fit[2] + fit[3] <= 1920); assertTrue(fit[1] + fit[4] <= 1080);
                assertEquals(left, (double) fit[2] / fit[4], .002);
                assertEquals(right, (double) fit[3] / fit[4], .002);
            }
        }
    }
}
