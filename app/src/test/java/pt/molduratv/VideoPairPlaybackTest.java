package pt.molduratv;

import org.junit.Test;
import static org.junit.Assert.*;

public class VideoPairPlaybackTest {
    @Test public void mutedRightMayFinishManyTimesWithoutCompletingItsAudibleTurn() {
        VideoPairPlayback pair = new VideoPairPlayback();
        for (int i = 0; i < 5; i++) {
            assertEquals(VideoPairPlayback.Action.RESTART_RIGHT, pair.ended(false));
            assertEquals(VideoPairPlayback.Phase.LEFT, pair.phase());
        }
        assertEquals(VideoPairPlayback.Action.START_RIGHT, pair.ended(true));
        assertEquals(VideoPairPlayback.Phase.RIGHT, pair.phase());
    }
    @Test public void rightGetsAFullTurnAndLeftPreviewCannotAdvanceThePair() {
        VideoPairPlayback pair = new VideoPairPlayback();
        assertEquals(VideoPairPlayback.Action.START_RIGHT, pair.ended(true));
        for (int i = 0; i < 4; i++) {
            assertEquals(VideoPairPlayback.Action.RESTART_LEFT, pair.ended(true));
            assertEquals(VideoPairPlayback.Phase.RIGHT, pair.phase());
        }
        assertEquals(VideoPairPlayback.Action.ADVANCE, pair.ended(false));
        assertEquals(VideoPairPlayback.Phase.COMPLETE, pair.phase());
    }
    @Test public void longerMutedRightStillRestartsBeforeItsAudibleTurn() {
        VideoPairPlayback pair = new VideoPairPlayback();
        assertEquals(VideoPairPlayback.Action.START_RIGHT, pair.ended(true));
        assertEquals(VideoPairPlayback.Action.ADVANCE, pair.ended(false));
    }
    @Test public void restoredRightTurnDoesNotGiveLeftAnotherAudibleTurn() {
        VideoPairPlayback pair = new VideoPairPlayback(VideoPairPlayback.Phase.RIGHT);
        assertEquals(VideoPairPlayback.Action.RESTART_LEFT, pair.ended(true));
        assertEquals(VideoPairPlayback.Action.ADVANCE, pair.ended(false));
    }
    @Test public void completedPairIgnoresLateEvents() {
        VideoPairPlayback pair = new VideoPairPlayback(VideoPairPlayback.Phase.COMPLETE);
        assertEquals(VideoPairPlayback.Action.NONE, pair.ended(true));
        assertEquals(VideoPairPlayback.Action.NONE, pair.ended(false));
    }
}
