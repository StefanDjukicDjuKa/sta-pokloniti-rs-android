package rs.stapokloniti.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class CircularProgressView extends View {

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int progress = 0;

    public CircularProgressView(Context context) {
        super(context);
        init();
    }

    public CircularProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CircularProgressView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        float density = getResources().getDisplayMetrics().density;

        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeWidth(8f * density);
        trackPaint.setStrokeCap(Paint.Cap.ROUND);
        trackPaint.setColor(Color.parseColor("#F4D7DE"));

        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(8f * density);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);
        progressPaint.setColor(Color.parseColor("#FF2D55"));

        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.parseColor("#1D2430"));
        textPaint.setFakeBoldText(true);
        textPaint.setTextSize(22f * density);
    }

    public void setProgress(int value) {
        progress = Math.max(0, Math.min(100, value));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float density = getResources().getDisplayMetrics().density;
        float stroke = 8f * density;
        float inset = stroke / 2f + 3f * density;

        RectF rect = new RectF(
                inset,
                inset,
                getWidth() - inset,
                getHeight() - inset
        );

        canvas.drawArc(rect, -90f, 360f, false, trackPaint);
        canvas.drawArc(rect, -90f, progress * 3.6f, false, progressPaint);

        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float baseline = getHeight() / 2f - (fm.ascent + fm.descent) / 2f;
        canvas.drawText(progress + "%", getWidth() / 2f, baseline, textPaint);
    }
}
