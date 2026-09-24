package kernel.unisocsu.chords_app.nativebridge;

import android.content.res.AssetManager;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import kernel.unisocsu.chords_app.model.TimedSegment;

public final class WhisperEngine implements AutoCloseable {
    private static final int SAMPLE_RATE = 16000;
    private static final int CHUNK_SECONDS = 60;
    private static final int OVERLAP_SECONDS = 1;
    private static final int CENTISECONDS_PER_SECOND = 100;
    private static final float SILENCE_RMS = 0.008f;

    private long context;

    public WhisperEngine(String modelPath) {
        context = NativeWhisper.initContext(modelPath);
        if (context == 0) throw new IllegalStateException("Whisper model could not be loaded");
    }

    public WhisperEngine(AssetManager assets, String assetPath) {
        context = NativeWhisper.initContextFromAsset(assets, assetPath);
        if (context == 0) throw new IllegalStateException("Whisper asset could not be loaded: " + assetPath);
    }

    public WhisperEngine(InputStream stream) {
        context = NativeWhisper.initContextFromInputStream(stream);
        if (context == 0) throw new IllegalStateException("Whisper model stream could not be loaded");
    }

    public List<TimedSegment> transcribe(float[] audio, int threads) {
        if (context == 0) throw new IllegalStateException("Whisper context released");
        NativeWhisper.fullTranscribe(context, threads, audio);
        return readSegments(0L);
    }

    /**
     * Processes bounded windows instead of one large audio buffer.
     * A one-second overlap preserves words crossing a window boundary;
     * segments inside the overlap are taken from the preceding window.
     */
    public List<TimedSegment> transcribeChunked(float[] audio, int threads) {
        if (context == 0) throw new IllegalStateException("Whisper context released");

        List<TimedSegment> result = new ArrayList<TimedSegment>();
        final int chunk = CHUNK_SECONDS * SAMPLE_RATE;
        final int overlap = OVERLAP_SECONDS * SAMPLE_RATE;
        final int step = chunk - overlap;

        if (audio.length <= chunk) {
            if (!isSilent(audio, 0, audio.length)) {
                NativeWhisper.fullTranscribe(context, threads, audio);
                result.addAll(readSegments(0L));
            }
            return result;
        }

        int start = 0;
        boolean first = true;
        while (start < audio.length) {
            int end = Math.min(audio.length, start + chunk);
            if (end <= start) break;

            if (!isSilent(audio, start, end)) {
                float[] window = new float[end - start];
                System.arraycopy(audio, start, window, 0, window.length);

                NativeWhisper.fullTranscribe(context, threads, window);

                long offsetCs = ((long) start * CENTISECONDS_PER_SECOND) / SAMPLE_RATE;
                long skipCs = first ? 0L : ((long) overlap * CENTISECONDS_PER_SECOND) / SAMPLE_RATE;

                int count = NativeWhisper.segmentCount(context);
                for (int i = 0; i < count; i++) {
                    long localStart = NativeWhisper.segmentStart(context, i);
                    if (localStart < skipCs) continue;

                    result.add(new TimedSegment(
                            localStart + offsetCs,
                            NativeWhisper.segmentEnd(context, i) + offsetCs,
                            NativeWhisper.segmentText(context, i)));
                }
            }

            if (end == audio.length) break;

            // If the remaining tail fits in one normal window, move the next
            // window to the end rather than creating a tiny third window.
            int next = start + step;
            if (audio.length - next <= chunk) {
                next = audio.length - chunk;
            }
            if (next <= start) break;
            start = next;
            first = false;
        }
        return result;
    }

    private boolean isSilent(float[] audio, int start, int end) {
        int length = end - start;
        if (length <= 0) return true;

        double sum = 0.0;
        int stride = Math.max(1, length / 8000);
        int count = 0;
        for (int i = start; i < end; i += stride) {
            float v = audio[i];
            sum += (double) v * v;
            count++;
        }
        return count == 0 || Math.sqrt(sum / count) < SILENCE_RMS;
    }

    private List<TimedSegment> readSegments(long offsetCs) {
        int count = NativeWhisper.segmentCount(context);
        List<TimedSegment> result = new ArrayList<TimedSegment>(count);
        for (int i = 0; i < count; i++) {
            result.add(new TimedSegment(
                    NativeWhisper.segmentStart(context, i) + offsetCs,
                    NativeWhisper.segmentEnd(context, i) + offsetCs,
                    NativeWhisper.segmentText(context, i)));
        }
        return result;
    }

    @Override public void close() {
        if (context != 0) {
            NativeWhisper.freeContext(context);
            context = 0;
        }
    }
}
