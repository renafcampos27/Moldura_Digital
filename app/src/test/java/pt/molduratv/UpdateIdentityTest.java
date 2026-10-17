package pt.molduratv;

import org.junit.Test;
import java.io.IOException;
import java.util.*;
import static org.junit.Assert.*;

public class UpdateIdentityTest {
    private final Set<String> trusted = new HashSet<>(Collections.singletonList("certificate-a"));
    private UpdateIdentity installed() { return new UpdateIdentity("pt.molduratv", "0.3.0", 3, 26, trusted); }
    @Test public void allowsNewerVersionWithSameIdentityAndSupportedAndroid() throws Exception {
        new UpdateIdentity("pt.molduratv", "0.4.0", 4, 26, trusted).validateAgainst(installed(), "v0.4.0", 26);
    }
    @Test public void rejectsDifferentAppAndMislabelledRelease() throws Exception {
        reject(new UpdateIdentity("other.app", "0.4.0", 4, 26, trusted));
        reject(new UpdateIdentity("pt.molduratv", "0.3.1", 4, 26, trusted));
    }
    @Test public void rejectsEqualOrLowerVersionCodeAndIncompatibleAndroid() throws Exception {
        reject(new UpdateIdentity("pt.molduratv", "0.4.0", 3, 26, trusted));
        reject(new UpdateIdentity("pt.molduratv", "0.4.0", 2, 26, trusted));
        reject(new UpdateIdentity("pt.molduratv", "0.4.0", 4, 37, trusted));
        try {
            new UpdateIdentity("pt.molduratv", "0.2.0", 10, 26, trusted).validateAgainst(installed(), "v0.2.0", 26);
            fail("Older release with a higher versionCode accepted");
        } catch (IOException expected) { }
    }
    @Test public void rejectsWrongMissingAndPartialMultiSignerIdentity() throws Exception {
        reject(new UpdateIdentity("pt.molduratv", "0.4.0", 4, 26, new HashSet<>(Collections.singletonList("certificate-b"))));
        reject(new UpdateIdentity("pt.molduratv", "0.4.0", 4, 26, Collections.emptySet()));
        reject(new UpdateIdentity("pt.molduratv", "0.4.0", 4, 26, new HashSet<>(Arrays.asList("certificate-a", "certificate-b"))));
    }
    private void reject(UpdateIdentity candidate) throws Exception {
        try { candidate.validateAgainst(installed(), "0.4.0", 26); fail("Unsafe update accepted"); }
        catch (IOException expected) { }
    }
}
