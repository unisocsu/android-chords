package kernel.unisocsu.chords_app.nativebridge;

import android.content.res.AssetManager;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import kernel.unisocsu.chords_app.model.TimedSegment;

public final class WhisperEngine implements AutoCloseable {
    private static final int SAMPLE_RATE = 16000;
    private static final int CHUNK_SECONDS = 45;
    private static final int OVERLAP_SECONDS = 1;
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
     * Processes bounded audio windows instead of passing a multi-minute buffer to
     * Whisper at once. A small overlap preserves words crossing a chunk boundary.
     * Very quiet chunks are skipped, reducing unnecessary CPU work on silence.
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

        for (int start = 0; start < audio.length; start += step) {
            int end = Math.min(audio.length, start + chunk);
            if (end <= start) break;
            if (!isSilent(audio, start, end)) {
                float[] window = new float[end - start];
                System.arraycopy(audio, start, window, 0, window.length);
                NativeWhisper.fullTranscribe(context, threads, window);
                List<TimedSegment> segments = readSegments(start * 1000L / SAMPLE_RATE);
                for (TimedSegment segment : segments) {
                    // The one-second overlap can produce duplicate boundary text.
                    // Keep a segment only when its absolute start is beyond the
                    // previous accepted segment's start.
                    if (result.isEmpty() ||
                            segment.getStartMs() > result.get(result.size() - 1).getStartMs()) {
                        result.add(segment);
                    }
                }
            }
            if (end == audio.length) break;
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

    private List<TimedSegment> readSegments(long offsetMs) {
        int count = NativeWhisper.segmentCount(context);
        List<TimedSegment> result = new ArrayList<TimedSegment>(count);
        for (int i = 0; i < count; i++) {
            result.add(new TimedSegment(
                    NativeWhisper.segmentStart(context, i) + offsetMs,
                    NativeWhisper.segmentEnd(context, i) + offsetMs,
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
