package kernel.unisocsu.chords_app.nativebridge;

import java.util.ArrayList;
import java.util.List;
import kernel.unisocsu.chords_app.model.Chord;

public final class ChordEngine {
    public List<Chord> detect(float[] audio, int sampleRate) {
        String[] raw = NativeChords.detect(audio, sampleRate);
        List<Chord> out = new ArrayList<Chord>(raw.length);
        for (String s : raw) {
            if (s == null) continue;
            String[] p = s.split("\\\\|", -1);
            if (p.length != 3) continue;
            try { out.add(new Chord(Double.parseDouble(p[0]), Double.parseDouble(p[1]), p[2])); }
            catch (NumberFormatException ignored) { }
        }
        return out;
    }
}
