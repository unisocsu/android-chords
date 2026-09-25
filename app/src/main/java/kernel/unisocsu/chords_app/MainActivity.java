package kernel.unisocsu.chords_app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;
import java.io.FileOutputStream;
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
    private static final int CREATE_PDF = 42;
    private static final String WHISPER_MODEL_ASSET = "ggml-tiny-q5_1.bin";
    private TextView status, result;
    private Button analyze;
    private ProgressBar progress;
    private Uri selectedUri;
    private String renderedSong;

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
        ((Button)findViewById(R.id.exportPdfButton)).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { exportPdf(); }
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
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;

        if (requestCode == PICK_AUDIO) {
            selectedUri = data.getData();
            status.setText("נבחר: " + selectedUri.toString());
            analyze.setEnabled(true);
        } else if (requestCode == CREATE_PDF) {
            savePdf(data.getData());
        }
    }

    private void exportPdf() {
        if (renderedSong == null || renderedSong.length() == 0) {
            Toast.makeText(this, "אין עדיין תוצאה לייצוא", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/pdf");
        i.putExtra(Intent.EXTRA_TITLE, "chords-result.pdf");
        try {
            startActivityForResult(i, CREATE_PDF);
        } catch (Exception e) {
            Toast.makeText(this, "לא ניתן לפתוח שמירת PDF", Toast.LENGTH_LONG).show();
        }
    }

    private void savePdf(final Uri uri) {
        final String text = renderedSong;
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    android.os.ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "w");
                    if (pfd == null) throw new java.io.IOException("Could not open destination");
                    try {
                        FileOutputStream out = new FileOutputStream(pfd.getFileDescriptor());
                        try {
                            PdfExporter.write(text, out);
                            out.flush();
                        } finally {
                            out.close();
                        }
                    } finally {
                        pfd.close();
                    }
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            Toast.makeText(MainActivity.this, "ה-PDF נשמר בהצלחה", Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (final Throwable e) {
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            Toast.makeText(MainActivity.this, "שגיאה בייצוא PDF: " + e.getClass().getSimpleName(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void analyzeSelected() {
        if (selectedUri == null) return;
        analyze.setEnabled(false);
        progress.setVisibility(View.VISIBLE);
        result.setText("");
        renderedSong = null;
        findViewById(R.id.exportPdfButton).setEnabled(false);
        status.setText("מפענח ומנתח…");
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    final float[] audio = AudioDecoder.decodeTo16kMono(MainActivity.this, selectedUri);
                    final List<Chord> chords = new ChordEngine().detect(audio, 16000);
                    WhisperEngine whisper = new WhisperEngine(getAssets(), WHISPER_MODEL_ASSET);
                    List<TimedSegment> words;
                    try {
                        words = whisper.transcribeChunked(audio, 2);
                    } finally {
                        whisper.close();
                    }
                    final String rendered = SongAssembler.render(words, chords);
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            progress.setVisibility(View.GONE);
                            analyze.setEnabled(true);
                            status.setText("הניתוח הסתיים");
                            renderedSong = rendered;
                            result.setText(rendered);
                            findViewById(R.id.exportPdfButton).setEnabled(true);
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
