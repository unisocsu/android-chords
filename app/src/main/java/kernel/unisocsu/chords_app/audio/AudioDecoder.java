package kernel.unisocsu.chords_app.audio;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public final class AudioDecoder {
    private static final int TARGET_RATE = 16000;
    private AudioDecoder() {}

    public static float[] decodeTo16kMono(Context context, Uri uri) throws Exception {
        MediaExtractor extractor = new MediaExtractor();
        extractor.setDataSource(context, uri, null);
        int track = -1;
        MediaFormat format = null;
        for (int i = 0; i < extractor.getTrackCount(); i++) {
            MediaFormat f = extractor.getTrackFormat(i);
            String mime = f.getString(MediaFormat.KEY_MIME);
            if (mime != null && mime.startsWith("audio/")) { track = i; format = f; break; }
        }
        if (track < 0 || format == null) throw new IOException("No audio track found");
        extractor.selectTrack(track);
        String mime = format.getString(MediaFormat.KEY_MIME);
        MediaCodec codec = MediaCodec.createDecoderByType(mime);
        codec.configure(format, null, null, 0);
        codec.start();

        int inputRate = format.containsKey(MediaFormat.KEY_SAMPLE_RATE)
                ? format.getInteger(MediaFormat.KEY_SAMPLE_RATE) : TARGET_RATE;
        int channels = format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)
                ? format.getInteger(MediaFormat.KEY_CHANNEL_COUNT) : 1;
        FloatBuilder pcm = new FloatBuilder();
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        boolean inputDone = false, outputDone = false;

        try {
            while (!outputDone) {
                if (!inputDone) {
                    int inIndex = codec.dequeueInputBuffer(10000);
                    if (inIndex >= 0) {
                        ByteBuffer in = codec.getInputBuffers()[inIndex];
                        in.clear();
                        int n = extractor.readSampleData(in, 0);
                        if (n < 0) {
                            codec.queueInputBuffer(inIndex, 0, 0, 0,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            inputDone = true;
                        } else {
                            codec.queueInputBuffer(inIndex, 0, n, extractor.getSampleTime(), 0);
                            extractor.advance();
                        }
                    }
                }

                int outIndex = codec.dequeueOutputBuffer(info, 10000);
                if (outIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED ||
                        outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) continue;

                if (outIndex >= 0) {
                    ByteBuffer out = codec.getOutputBuffers()[outIndex];
                    out.position(info.offset);
                    out.limit(info.offset + info.size);
                    appendPcm16(out, pcm, channels, inputRate);
                    codec.releaseOutputBuffer(outIndex, false);
                    if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) outputDone = true;
                }
            }
        } finally {
            try { codec.stop(); } catch (Exception ignored) {}
            codec.release();
            extractor.release();
        }
        return pcm.toArray();
    }

    private static void appendPcm16(ByteBuffer data, FloatBuilder out, int channels, int rate) {
        int frames = data.remaining() / 2 / Math.max(1, channels);
        short[] mono = new short[frames];
        for (int i = 0; i < frames; i++) {
            long sum = 0;
            for (int c = 0; c < channels; c++) sum += data.getShort();
            mono[i] = (short)(sum / Math.max(1, channels));
        }
        if (rate == TARGET_RATE) {
            for (short s : mono) out.add(s / 32768.0f);
            return;
        }
        double step = rate / (double)TARGET_RATE;
        int targetCount = (int)Math.floor(mono.length / step);
        for (int i = 0; i < targetCount; i++) {
            double src = i * step;
            int a = (int)src;
            int b = Math.min(a + 1, mono.length - 1);
            double t = src - a;
            out.add((float)(((1.0 - t) * mono[a] + t * mono[b]) / 32768.0));
        }
    }

    private static final class FloatBuilder {
        private float[] data = new float[16384];
        private int size;
        void add(float v) {
            if (size == data.length) data = Arrays.copyOf(data, data.length * 2);
            data[size++] = v;
        }
        float[] toArray() { return Arrays.copyOf(data, size); }
    }
}
