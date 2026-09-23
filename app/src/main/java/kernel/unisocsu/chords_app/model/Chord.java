package kernel.unisocsu.chords_app.model;

public final class Chord {
    public final double startSec;
    public final double endSec;
    public final String label;
    public Chord(double startSec, double endSec, String label) {
        this.startSec = startSec;
        this.endSec = endSec;
        this.label = label;
    }
}
