package kernel.unisocsu.chords_app.nativebridge;

public final class NativeChords {
    static { System.loadLibrary("whisper"); }
    private NativeChords() {}
    public static native String[] detect(float[] audio, int sampleRate);
    public static native String classifyFrame(float[] frame, int sampleRate);
    public static native int frameLengthSamples(int sampleRate);
}
