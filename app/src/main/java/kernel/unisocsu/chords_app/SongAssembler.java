package kernel.unisocsu.chords_app;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import kernel.unisocsu.chords_app.model.Chord;
import kernel.unisocsu.chords_app.model.TimedSegment;

public final class SongAssembler {
    private SongAssembler() {}

    public static String render(List<TimedSegment> words, List<Chord> chords) {
        Collections.sort(words, new Comparator<TimedSegment>() {
            @Override public int compare(TimedSegment a, TimedSegment b) {
                return Long.compare(a.startCs, b.startCs);
            }
        });
        Collections.sort(chords, new Comparator<Chord>() {
            @Override public int compare(Chord a, Chord b) {
                return Double.compare(a.startSec, b.startSec);
            }
        });
        StringBuilder out = new StringBuilder();
        int ci = 0;
        for (TimedSegment word : words) {
            double t = word.startCs / 100.0;
            while (ci < chords.size() && chords.get(ci).startSec <= t) {
                Chord c = chords.get(ci++);
                if (!"N".equals(c.label)) out.append('[').append(c.label).append("] ");
            }
            out.append(word.text.trim()).append(' ');
        }
        return out.toString().trim();
    }
}
