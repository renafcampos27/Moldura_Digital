package pt.molduratv;

/** Each original gets one full audible turn; muted previews do not complete the pair. */
public final class VideoPairPlayback {
    public enum Phase { LEFT, RIGHT, COMPLETE }
    public enum Action { RESTART_LEFT, RESTART_RIGHT, START_RIGHT, ADVANCE, NONE }
    private Phase phase;
    public VideoPairPlayback() { this(Phase.LEFT); }
    public VideoPairPlayback(Phase phase) { this.phase = java.util.Objects.requireNonNull(phase); }
    public Phase phase() { return phase; }
    public Action ended(boolean left) {
        if (phase == Phase.COMPLETE) return Action.NONE;
        if (left) {
            if (phase == Phase.LEFT) { phase = Phase.RIGHT; return Action.START_RIGHT; }
            return Action.RESTART_LEFT;
        }
        if (phase == Phase.LEFT) return Action.RESTART_RIGHT;
        phase = Phase.COMPLETE; return Action.ADVANCE;
    }
}
