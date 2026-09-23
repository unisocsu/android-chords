package kernel.unisocsu.chords_app.nativebridge;

import android.content.res.AssetManager;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import kernel.unisocsu.chords_app.model.TimedSegment;

public final class WhisperEngine implements AutoCloseable {
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
        int count = NativeWhisper.segmentCount(context);
        List<TimedSegment> result = new ArrayList<TimedSegment>(count);
        for (int i = 0; i < count; i++) {
            result.add(new TimedSegment(
                    NativeWhisper.segmentStart(context, i),
                    NativeWhisper.segmentEnd(context, i),
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
