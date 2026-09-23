package kernel.unisocsu.chords_app.nativebridge;

import android.content.res.AssetManager;
import java.io.InputStream;

public final class NativeWhisper {
    static { System.loadLibrary("whisper"); }
    private NativeWhisper() {}
    public static native long initContext(String modelPath);
    public static native long initContextFromAsset(AssetManager assets, String assetPath);
    public static native long initContextFromInputStream(InputStream stream);
    public static native void freeContext(long context);
    public static native void fullTranscribe(long context, int threads, float[] audio);
    public static native int segmentCount(long context);
    public static native String segmentText(long context, int index);
    public static native long segmentStart(long context, int index);
    public static native long segmentEnd(long context, int index);
    public static native String systemInfo();
}
