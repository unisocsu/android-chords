package kernel.unisocsu.chords_app;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import java.io.FileOutputStream;
import java.io.IOException;

public final class PdfExporter {
    private static final int PAGE_WIDTH = 595;
    private static final int PAGE_HEIGHT = 842;
    private static final int MARGIN = 42;
    private static final float TEXT_SIZE = 12f;
    private static final float LINE_HEIGHT = 18f;

    private PdfExporter() { }

    public static void write(String text, FileOutputStream output) throws IOException {
        PdfDocument document = new PdfDocument();
        try {
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setTextSize(TEXT_SIZE);
            paint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL));

            String[] lines = text == null ? new String[] { "" } : text.split("\\n", -1);
            PdfDocument.Page page = null;
            float y = MARGIN;
            int pageNumber = 1;

            for (String line : lines) {
                if (page == null || y + LINE_HEIGHT > PAGE_HEIGHT - MARGIN) {
                    if (page != null) document.finishPage(page);
                    page = document.startPage(new PdfDocument.PageInfo.Builder(
                            PAGE_WIDTH, PAGE_HEIGHT, pageNumber++).create());
                    y = MARGIN;
                }
                canvasDraw(page.getCanvas(), line.length() == 0 ? " " : line, paint, y);
                y += LINE_HEIGHT;
            }

            if (page != null) document.finishPage(page);
            document.writeTo(output);
        } finally {
            document.close();
        }
    }

    private static void canvasDraw(Canvas canvas, String line, Paint paint, float y) {
        canvas.drawText(line, MARGIN, y, paint);
    }
}
