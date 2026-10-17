package pt.molduratv;

import java.time.*;
import java.time.format.*;

/** No invented capture dates: invalid/missing metadata falls back explicitly to file modification. */
public final class CaptureDates {
    private static final DateTimeFormatter EXIF=DateTimeFormatter.ofPattern("uuuu:MM:dd HH:mm:ss").withResolverStyle(ResolverStyle.STRICT);
    private CaptureDates() {}
    public static long exif(String date,String offset,String subseconds) {
        if(date==null) return 0;
        try {
            LocalDateTime local=LocalDateTime.parse(date.trim(),EXIF);
            if(subseconds!=null && subseconds.matches("[0-9]{1,9}")) local=local.withNano(Integer.parseInt((subseconds+"000000000").substring(0,9)));
            return positive(offset==null || offset.trim().isEmpty()?local.atZone(ZoneId.systemDefault()).toInstant():local.toInstant(ZoneOffset.of(offset.trim())));
        } catch(DateTimeException | NumberFormatException invalid) { return 0; }
    }
    public static long video(String value) {
        if(value==null) return 0;
        try { return positive(Instant.parse(value)); } catch(DateTimeException ignored) { }
        try { return positive(OffsetDateTime.parse(value,DateTimeFormatter.ofPattern("uuuuMMdd'T'HHmmss[.SSS]XX").withResolverStyle(ResolverStyle.STRICT)).toInstant()); }
        catch(DateTimeException ignored) { return 0; }
    }
    private static long positive(Instant instant) { try { return Math.max(0,instant.toEpochMilli()); } catch(ArithmeticException invalid) { return 0; } }
    public static long orderKey(long captured,long modified) { return captured>0?captured:modified>0?modified:Long.MAX_VALUE; }
}
