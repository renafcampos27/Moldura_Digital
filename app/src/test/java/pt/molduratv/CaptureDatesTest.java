package pt.molduratv;

import org.junit.Test;
import java.time.*;
import static org.junit.Assert.*;

public class CaptureDatesTest {
    @Test public void readsExifOffsetAndSubsecond() {
        assertEquals(Instant.parse("2024-03-20T11:30:45.123Z").toEpochMilli(),CaptureDates.exif("2024:03:20 12:30:45","+01:00","123"));
    }
    @Test public void missingOffsetUsesLocalZoneAndMissingFractionUsesZero() {
        assertEquals(LocalDateTime.of(2024,3,20,12,30,45).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),CaptureDates.exif("2024:03:20 12:30:45",null,null));
    }
    @Test public void invalidAndImpossibleExifDatesDoNotBecomeCaptureDates() {
        assertEquals(0,CaptureDates.exif("2024:02:30 12:00:00",null,null)); assertEquals(0,CaptureDates.exif(null,null,null));
        assertEquals(0,CaptureDates.exif("2024:03:20 12:00:00","bad",null));
    }
    @Test public void readsIsoAndAndroidVideoDateFormats() {
        long expected=Instant.parse("2024-03-20T12:30:45.123Z").toEpochMilli();
        assertEquals(expected,CaptureDates.video("2024-03-20T12:30:45.123Z")); assertEquals(expected,CaptureDates.video("20240320T123045.123Z"));
        assertEquals(expected-123,CaptureDates.video("20240320T133045+0100"));
    }
    @Test public void rejectsUnknownVideoDatesAndFallbackIsExplicit() {
        assertEquals(0,CaptureDates.video("invalid")); assertEquals(0,CaptureDates.video("20240230T120000Z"));
        assertEquals(10,CaptureDates.orderKey(10,20)); assertEquals(20,CaptureDates.orderKey(0,20)); assertEquals(Long.MAX_VALUE,CaptureDates.orderKey(0,0));
    }
}
