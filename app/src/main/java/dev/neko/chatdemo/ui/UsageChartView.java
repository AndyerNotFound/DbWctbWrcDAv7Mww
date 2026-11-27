package dev.neko.chatdemo.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.color.MaterialColors;

/** A compact animated bar chart for session throughput. */
public class UsageChartView extends View {

    private static final int[] VALUES = {
            420, 530, 610, 480, 720, 860, 940, 810, 700, 1120, 1280, 1180, 1420, 1610,
            1380, 1240, 1100, 1320, 1540, 1720, 1620, 1450, 1180, 980, 860, 740, 620, 560
    };

    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint headPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    private int colorPrimary;
    private int colorTertiary;
    private int colorOutline;
    private int colorOnSurfaceVariant;
    private float dp;
    private float progress = 1f;

    public UsageChartView(Context context) {
        this(context, null);
    }

    public UsageChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        dp = getResources().getDisplayMetrics().density;
        colorPrimary = resolve(androidx.appcompat.R.attr.colorPrimary);
        colorTertiary = resolve(com.google.android.material.R.attr.colorTertiary);
        colorOutline = resolve(com.google.android.material.R.attr.colorOutlineVariant);
        colorOnSurfaceVariant = resolve(com.google.android.material.R.attr.colorOnSurfaceVariant);

        textPaint.setColor(colorOnSurfaceVariant);
        textPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 9.5f,
                getResources().getDisplayMetrics()));
        textPaint.setTypeface(android.graphics.Typeface.create("sans-serif-medium",
                android.graphics.Typeface.NORMAL));
        linePaint.setStrokeWidth(dp);
        linePaint.setColor(colorOutline);
    }

    private int resolve(int attr) {
        return MaterialColors.getColor(this, attr);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (isInEditMode()) {
            return;
        }
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(720);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(a -> {
            progress = (float) a.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) {
            return;
        }

        float labelSpace = dp * 16f;
        float top = dp * 10f;
        float baseline = h - labelSpace;
        float chartH = baseline - top;
        float slot = (w - dp * 4f) / VALUES.length;
        float barW = Math.max(dp * 3.2f, slot * 0.58f);
        float radius = Math.min(barW / 2f, dp * 3.5f);

        // Baseline + average line.
        canvas.drawLine(dp, baseline, w - dp, baseline, linePaint);
        float average = 0f;
        int max = 0;
        for (int v : VALUES) {
            average += v;
            max = Math.max(max, v);
        }
        average /= VALUES.length;
        float avgY = baseline - (average / max) * chartH;

        linePaint.setColor(colorOutline);
        linePaint.setAlpha(170);
        linePaint.setPathEffect(new android.graphics.DashPathEffect(new float[]{dp * 3f, dp * 4f}, 0));
        canvas.drawLine(dp, avgY, w - dp, avgY, linePaint);
        linePaint.setPathEffect(null);

        textPaint.setColor(colorOnSurfaceVariant);
        canvas.drawText("avg", dp * 2f, avgY - dp * 3f, textPaint);

        for (int i = 0; i < VALUES.length; i++) {
            float value = VALUES[i] / (float) max;
            float barH = Math.max(dp * 2f, value * chartH * progress);
            float left = dp * 2f + i * slot + (slot - barW) / 2f;
            rect.set(left, baseline - barH, left + barW, baseline);

            if (i == VALUES.length - 1) {
                barPaint.setShader(null);
                barPaint.setColor(colorTertiary);
                headPaint.setShader(new LinearGradient(0, rect.top, 0, baseline,
                        colorTertiary, adjustAlpha(colorTertiary, 80), Shader.TileMode.CLAMP));
                canvas.drawRoundRect(rect, radius, radius, headPaint);
            } else {
                barPaint.setShader(new LinearGradient(0, rect.top, 0, baseline,
                        colorPrimary, adjustAlpha(colorPrimary, 70), Shader.TileMode.CLAMP));
                canvas.drawRoundRect(rect, radius, radius, barPaint);
            }
        }
        barPaint.setShader(null);

        textPaint.setColor(colorOnSurfaceVariant);
        canvas.drawText("0", dp, h - dp * 3f, textPaint);
        String peak = "1.8k";
        canvas.drawText(peak, w - dp * 2f - textPaint.measureText(peak), h - dp * 3f, textPaint);
    }

    private int adjustAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }
}
