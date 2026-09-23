package kernel.unisocsu.chords_app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.util.List;
import kernel.unisocsu.chords_app.audio.AudioDecoder;
import kernel.unisocsu.chords_app.model.Chord;
import kernel.unisocsu.chords_app.model.TimedSegment;
import kernel.unisocsu.chords_app.nativebridge.ChordEngine;
import kernel.unisocsu.chords_app.nativebridge.WhisperEngine;

public final class MainActivity extends Activity {
    private static final int PICK_AUDIO = 41;
    private TextView status, result;
    private Button analyze;
    private ProgressBar progress;
    private Uri selectedUri;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        status = (TextView)findViewById(R.id.status);
        result = (TextView)findViewById(R.id.result);
        analyze = (Button)findViewById(R.id.analyzeButton);
        progress = (ProgressBar)findViewById(R.id.progress);
        ((Button)findViewById(R.id.selectButton)).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { pickAudio(); }
        });
        analyze.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { analyzeSelected(); }
        });
    }

    private void pickAudio() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("audio/*");
        try {
            startActivityForResult(i, PICK_AUDIO);
        } catch (Exception e) {
            i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("audio/*");
            startActivityForResult(i, PICK_AUDIO);
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_AUDIO && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedUri = data.getData();
            status.setText("נבחר: " + selectedUri.toString());
            analyze.setEnabled(true);
        }
    }

    private void analyzeSelected() {
        if (selectedUri == null) return;
        analyze.setEnabled(false);
        progress.setVisibility(View.VISIBLE);
        result.setText("");
        status.setText("מפענח ומנתח…");
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    final float[] audio = AudioDecoder.decodeTo16kMono(MainActivity.this, selectedUri);
                    final List<Chord> chords = new ChordEngine().detect(audio, 16000);
                    WhisperEngine whisper = new WhisperEngine("/sdcard/ChordsApp/model.bin");
                    List<TimedSegment> words;
                    try {
                        words = whisper.transcribe(audio, 2);
                    } finally {
                        whisper.close();
                    }
                    final String rendered = SongAssembler.render(words, chords);
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            progress.setVisibility(View.GONE);
                            analyze.setEnabled(true);
                            status.setText("הניתוח הסתיים");
                            result.setText(rendered);
                        }
                    });
                } catch (final Throwable e) {
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            progress.setVisibility(View.GONE);
                            analyze.setEnabled(true);
                            status.setText("שגיאה: " + e.getClass().getSimpleName());
                            result.setText(String.valueOf(e.getMessage()));
                        }
                    });
                }
            }
        }).start();
    }
}
