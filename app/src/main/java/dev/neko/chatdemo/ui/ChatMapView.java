package dev.neko.chatdemo.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.color.MaterialColors;

import java.util.ArrayList;
import java.util.List;

/**
 * A tiny conversation graph. Nodes are laid out in normalized coordinates and connected with
 * curved links; the live head keeps pulsing.
 */
public class ChatMapView extends View {

    private static final int KIND_USER = 0;
    private static final int KIND_NEKO = 1;
    private static final int KIND_TOOL = 2;
    private static final int KIND_HEAD = 3;

    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint branchPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mainPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint captionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pulsePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Path path = new Path();
    private final RectF card = new RectF();
    private final List<Node> nodes = new ArrayList<>();
    private final List<Edge> edges = new ArrayList<>();

    private int colorPrimary;
    private int colorOnPrimary;
    private int colorPrimaryContainer;
    private int colorOnPrimaryContainer;
    private int colorSurface;
    private int colorSurfaceContainerHigh;
    private int colorOnSurface;
    private int colorOnSurfaceVariant;
    private int colorOutlineVariant;
    private int colorTertiary;
    private int colorTertiaryContainer;
    private int colorOnTertiaryContainer;

    private float dp;
    private long pulseOffset;
    private float intro = 1f;

    public ChatMapView(Context context) {
        this(context, null);
    }

    public ChatMapView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        dp = getResources().getDisplayMetrics().density;
        resolveColors();
        buildGraph();
    }

    private void resolveColors() {
        colorPrimary = color(androidx.appcompat.R.attr.colorPrimary);
        colorOnPrimary = color(com.google.android.material.R.attr.colorOnPrimary);
        colorPrimaryContainer = color(com.google.android.material.R.attr.colorPrimaryContainer);
        colorOnPrimaryContainer = color(com.google.android.material.R.attr.colorOnPrimaryContainer);
        colorSurface = color(com.google.android.material.R.attr.colorSurface);
        colorSurfaceContainerHigh = color(com.google.android.material.R.attr.colorSurfaceContainerHigh);
        colorOnSurface = color(com.google.android.material.R.attr.colorOnSurface);
        colorOnSurfaceVariant = color(com.google.android.material.R.attr.colorOnSurfaceVariant);
        colorOutlineVariant = color(com.google.android.material.R.attr.colorOutlineVariant);
        colorTertiary = color(com.google.android.material.R.attr.colorTertiary);
        colorTertiaryContainer = color(com.google.android.material.R.attr.colorTertiaryContainer);
        colorOnTertiaryContainer = color(com.google.android.material.R.attr.colorOnTertiaryContainer);

        textPaint.setTextSize(sp(11.5f));
        textPaint.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
        textPaint.setTextAlign(Paint.Align.CENTER);

        captionPaint.setTextSize(sp(9.5f));
        captionPaint.setColor(colorOnSurfaceVariant);
        captionPaint.setTextAlign(Paint.Align.CENTER);

        dotPaint.setColor(colorOutlineVariant);
        dotPaint.setAlpha(110);

        branchPaint.setStyle(Paint.Style.STROKE);
        branchPaint.setStrokeWidth(dp * 1.6f);
        branchPaint.setColor(colorOutlineVariant);
        branchPaint.setStrokeCap(Paint.Cap.ROUND);

        mainPaint.setStyle(Paint.Style.STROKE);
        mainPaint.setStrokeWidth(dp * 2.6f);
        mainPaint.setStrokeCap(Paint.Cap.ROUND);

        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeWidth(dp * 9f);
        glowPaint.setStrokeCap(Paint.Cap.ROUND);
        glowPaint.setColor(colorPrimary);
        glowPaint.setAlpha(30);

        cardPaint.setStyle(Paint.Style.FILL);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(dp);
        pulsePaint.setStyle(Paint.Style.STROKE);
        pulsePaint.setStrokeWidth(dp * 1.5f);
    }

    private int color(int attr) {
        return MaterialColors.getColor(this, attr);
    }

    private float sp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value,
                getResources().getDisplayMetrics());
    }

    private void buildGraph() {
        nodes.clear();
        edges.clear();
        // Normalized positions inside the drawing area.
        nodes.add(new Node(0.10f, 0.04f, "Start", KIND_USER));
        nodes.add(new Node(0.36f, 0.16f, "Dock", KIND_NEKO));
        nodes.add(new Node(0.68f, 0.10f, "Tabs", KIND_NEKO));
        nodes.add(new Node(0.86f, 0.30f, "XML audit", KIND_TOOL));
        nodes.add(new Node(0.56f, 0.40f, "Insets", KIND_NEKO));
        nodes.add(new Node(0.24f, 0.50f, "Sketch", KIND_USER));
        nodes.add(new Node(0.14f, 0.66f, "Pill tabs", KIND_NEKO));
        nodes.add(new Node(0.80f, 0.62f, "Screens", KIND_TOOL));
        nodes.add(new Node(0.52f, 0.76f, "Polish", KIND_NEKO));
        nodes.add(new Node(0.36f, 0.92f, "Live head", KIND_HEAD));

        edges.add(new Edge(0, 1, true));
        edges.add(new Edge(1, 2, true));
        edges.add(new Edge(2, 3, false));
        edges.add(new Edge(2, 4, true));
        edges.add(new Edge(4, 5, true));
        edges.add(new Edge(4, 8, true));
        edges.add(new Edge(5, 6, false));
        edges.add(new Edge(8, 7, false));
        edges.add(new Edge(6, 9, false));
        edges.add(new Edge(8, 9, true));
    }

    public void pulseHead() {
        pulseOffset = 0;
        if (getWidth() > 0) {
            pulseOffset = -System.currentTimeMillis();
        }
        invalidate();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        pulseOffset = -System.currentTimeMillis();
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(650);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(a -> {
            intro = (float) a.getAnimatedValue();
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

        canvas.drawColor(colorSurface);

        // Dot grid.
        float step = dp * 22f;
        for (float x = step; x < w; x += step) {
            for (float y = step; y < h; y += step) {
                canvas.drawCircle(x, y, dp * 1.05f, dotPaint);
            }
        }

        RectF area = new RectF(dp * 18f, dp * 118f, w - dp * 18f, h - dp * 88f);
        if (area.width() <= 0 || area.height() <= 0) {
            area.set(dp * 18f, dp * 118f, Math.max(dp * 19f, w - dp * 18f), Math.max(dp * 119f, h - dp * 88f));
        }

        float[] cx = new float[nodes.size()];
        float[] cy = new float[nodes.size()];
        for (int i = 0; i < nodes.size(); i++) {
            Node n = nodes.get(i);
            cx[i] = area.left + n.x * area.width();
            cy[i] = area.top + n.y * area.height();
        }

        // Slow reveal of the links.
        long now = System.currentTimeMillis();
        float pulse = (float) ((Math.sin((now + pulseOffset) / 420.0) + 1) / 2.0);

        // Edges.
        for (Edge e : edges) {
            Node a = nodes.get(e.from);
            Node b = nodes.get(e.to);
            path.reset();
            path.moveTo(cx[e.from], cy[e.from]);
            float midY = (cy[e.from] + cy[e.to]) / 2f;
            path.cubicTo(cx[e.from], midY, cx[e.to], midY, cx[e.to], cy[e.to]);

            if (e.main) {
                canvas.drawPath(path, glowPaint);
                mainPaint.setShader(new LinearGradient(cx[e.from], cy[e.from], cx[e.to], cy[e.to],
                        colorPrimary, colorTertiary, Shader.TileMode.CLAMP));
                canvas.drawPath(path, mainPaint);
                mainPaint.setShader(null);
            } else {
                canvas.drawPath(path, branchPaint);
            }

            if (!e.main && e.from == 5) {
                canvas.drawText("pill tabs", (cx[e.from] + cx[e.to]) / 2f, midY - dp * 5f, captionPaint);
            }
            if (!e.main && e.from == 8) {
                canvas.drawText("retry", (cx[e.from] + cx[e.to]) / 2f, midY - dp * 5f, captionPaint);
            }
        }

        // Nodes.
        for (int i = 0; i < nodes.size(); i++) {
            Node n = nodes.get(i);
            drawNode(canvas, n, cx[i], cy[i], pulse, i == nodes.size() - 1);
        }

        // Corner caption.
        captionPaint.setTextAlign(Paint.Align.LEFT);
        captionPaint.setColor(colorOnSurfaceVariant);
        canvas.drawText("read-only graph · the live head keeps pulsing", dp * 18f, h - dp * 24f, captionPaint);
        captionPaint.setTextAlign(Paint.Align.CENTER);

        if (isAttachedToWindow()) {
            postInvalidateOnAnimation();
        }
    }

    private void drawNode(Canvas canvas, Node n, float x, float y, float pulse, boolean isHead) {
        textPaint.setTextSize(sp(isHead ? 12f : 11.5f));
        float textWidth = textPaint.measureText(n.label);
        float width = textWidth + dp * 26f;
        float height = dp * 34f;
        card.set(x - width / 2f, y - height / 2f, x + width / 2f, y + height / 2f);

        int fill;
        int text;
        int stroke = Color.TRANSPARENT;
        if (n.kind == KIND_USER) {
            fill = colorPrimary;
            text = colorOnPrimary;
        } else if (n.kind == KIND_HEAD) {
            fill = colorPrimaryContainer;
            text = colorOnPrimaryContainer;
            stroke = colorPrimary;
        } else if (n.kind == KIND_TOOL) {
            fill = colorTertiaryContainer;
            text = colorOnTertiaryContainer;
        } else {
            fill = colorSurfaceContainerHigh;
            text = colorOnSurface;
        }

        if (n.kind == KIND_HEAD) {
            float ring = dp * (13f + pulse * 7f);
            pulsePaint.setColor(colorPrimary);
            pulsePaint.setAlpha((int) (90 * (1f - pulse) + 30));
            canvas.drawCircle(x, y, ring, pulsePaint);
            pulsePaint.setAlpha(160);
            canvas.drawCircle(x, y, dp * 6f, pulsePaint);
        }

        cardPaint.setColor(fill);
        cardPaint.setAlpha((int) (255 * intro));
        canvas.drawRoundRect(card, dp * 14f, dp * 14f, cardPaint);
        if (stroke != Color.TRANSPARENT) {
            strokePaint.setColor(stroke);
            strokePaint.setAlpha((int) (255 * intro));
            canvas.drawRoundRect(card, dp * 14f, dp * 14f, strokePaint);
        }

        textPaint.setColor(text);
        textPaint.setAlpha((int) (255 * intro));
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float baseline = y - (fm.ascent + fm.descent) / 2f;
        canvas.drawText(n.label, x, baseline, textPaint);

        if (isHead) {
            captionPaint.setColor(colorTertiary);
            captionPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("LIVE", x, card.top - dp * 8f, captionPaint);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    private static class Node {
        final float x;
        final float y;
        final String label;
        final int kind;

        Node(float x, float y, String label, int kind) {
            this.x = x;
            this.y = y;
            this.label = label;
            this.kind = kind;
        }
    }

    private static class Edge {
        final int from;
        final int to;
        final boolean main;

        Edge(int from, int to, boolean main) {
            this.from = from;
            this.to = to;
            this.main = main;
        }
    }
}
