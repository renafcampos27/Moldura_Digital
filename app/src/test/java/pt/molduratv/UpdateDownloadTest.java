package pt.molduratv;

import org.junit.Test;
import java.io.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.*;

public class UpdateDownloadTest {
    private ReleaseUpdate release(byte[] bytes) throws Exception { return ReleaseUpdate.parse(ReleaseUpdateTest.release(bytes).toString(), "0.3.0"); }
    @Test public void streamsBytesAndVerifiesFullLengthAndSha256() throws Exception {
        byte[] source = new byte[150_000]; new java.util.Random(3).nextBytes(source);
        ByteArrayOutputStream destination = new ByteArrayOutputStream();
        long[] received = {0};
        GitHubUpdates.copyVerified(new ByteArrayInputStream(source), destination, release(source), new AtomicBoolean(), count -> received[0] = count);
        assertArrayEquals(source, destination.toByteArray()); assertEquals(source.length, received[0]);
    }
    @Test public void rejectsTruncatedOversizedAndAlteredTransfers() throws Exception {
        ReleaseUpdate update = release(new byte[]{1,2,3});
        for (byte[] invalid : new byte[][]{new byte[]{1,2}, new byte[]{1,2,3,4}, new byte[]{1,2,4}}) {
            try {
                GitHubUpdates.copyVerified(new ByteArrayInputStream(invalid), new ByteArrayOutputStream(), update, new AtomicBoolean(), count -> {});
                fail("Corrupt transfer accepted");
            } catch (IOException expected) { }
        }
    }
    @Test public void honoursCancellationBeforeAndDuringTransfer() throws Exception {
        byte[] bytes = new byte[150_000]; ReleaseUpdate update = release(bytes);
        try {
            GitHubUpdates.copyVerified(new ByteArrayInputStream(bytes), new ByteArrayOutputStream(), update, new AtomicBoolean(true), count -> {});
            fail();
        } catch (CancellationException expected) { }
        AtomicBoolean stop = new AtomicBoolean();
        try {
            GitHubUpdates.copyVerified(new ByteArrayInputStream(bytes), new ByteArrayOutputStream(), update, stop, count -> stop.set(true));
            fail();
        } catch (CancellationException expected) { }
    }
}
