package kernel.unisocsu.chords_app.model;

public final class TimedSegment {
    public final long startCs;
    public final long endCs;
    public final String text;
    public TimedSegment(long startCs, long endCs, String text) {
        this.startCs = startCs;
        this.endCs = endCs;
        this.text = text;
    }
}
