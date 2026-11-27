package dev.neko.chatdemo.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.RippleDrawable;
import android.util.AttributeSet;
import android.view.animation.PathInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;

import dev.neko.chatdemo.R;

/**
 * The user message surface.
 *
 * <p>Material 3 buttons are text widgets: they carry a label and at most two icons, so they cannot
 * host a thumbnail, a caption and a row of chips. This view reproduces the button surface instead —
 * the filled container, the state layer, the bounded ripple and the {@code Button} accessibility
 * role — while staying a layout, so a message can hold any rich media inside it.
 *
 * <p>Pressing it behaves like an M3 button rather than a list row: the corners morph from the
 * rounded bubble shape towards the small pressed shape and settle back on release. There is no
 * haptic tick — the shape is the feedback.
 *
 * <pre>
 * &lt;dev.neko.chatdemo.ui.BubbleButton
 *     app:bubbleSharpCorner="bottomRight"
 *     app:bubbleContainerColor="?attr/colorPrimaryContainer" /&gt;
 * </pre>
 */
public class BubbleButton extends LinearLayout {

    public static final int SHARP_NONE = 0;
    public static final int SHARP_BOTTOM_RIGHT = 1;
    public static final int SHARP_BOTTOM_LEFT = 2;

    /** The pressed corner radius, from the M3 button's small container shape. */
    private static final float PRESSED_RADIUS_DP = 8f;

    private final MaterialShapeDrawable fill;
    private final MaterialShapeDrawable mask;

    private final float radius;
    private final float tail;
    private final int sharpCorner;

    @Nullable
    private ValueAnimator morph;
    /** 0 = resting bubble shape, 1 = fully pressed shape. */
    private float morphProgress;
    private boolean pressed;

    public BubbleButton(Context context) {
        this(context, null);
    }

    public BubbleButton(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BubbleButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(VERTICAL);
        // The shape morph is the press feedback; a buzz on top of it reads as a different widget.
        setHapticFeedbackEnabled(false);

        TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.BubbleButton, defStyleAttr, 0);
        radius = a.getDimension(R.styleable.BubbleButton_bubbleCornerSize, dp(20));
        tail = a.getDimension(R.styleable.BubbleButton_bubbleTailSize, dp(5));
        sharpCorner = a.getInt(R.styleable.BubbleButton_bubbleSharpCorner, SHARP_BOTTOM_RIGHT);
        int container = a.getColor(R.styleable.BubbleButton_bubbleContainerColor,
                MaterialColors.getColor(this,
                        com.google.android.material.R.attr.colorPrimaryContainer));
        a.recycle();

        ShapeAppearanceModel shape = shapeFor(morphProgress);

        fill = new MaterialShapeDrawable(shape);
        fill.setFillColor(containerState(container));
        fill.setElevation(0f);

        // The ripple is clipped to the outline, so the press never spills past the rounded bubble.
        mask = new MaterialShapeDrawable(shape);
        mask.setFillColor(ColorStateList.valueOf(Color.WHITE));

        setBackground(new RippleDrawable(rippleState(), fill, mask));

        setClickable(true);
        setFocusable(true);
    }

    /** Retints the bubble, e.g. while the message it belongs to is being edited. */
    public void setContainerColor(@ColorInt int color) {
        fill.setFillColor(containerState(color));
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return Button.class.getName();
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        // The framework can refresh the drawable state while the view is still being constructed,
        // before the shape drawables exist.
        if (fill == null || mask == null) {
            return;
        }
        boolean nowPressed = isPressed();
        if (nowPressed != pressed) {
            pressed = nowPressed;
            morphTo(nowPressed);
        }
    }

    /**
     * Press: corners snap towards the small shape in a short, mostly linear move. Release: they
     * spring back with a slight overshoot, which is what makes the bubble feel elastic.
     */
    private void morphTo(boolean down) {
        if (morph != null) {
            morph.cancel();
        }
        final float from = morphProgress;
        final float to = down ? 1f : 0f;
        morph = ValueAnimator.ofFloat(from, to);
        morph.setDuration(down ? 120 : 320);
        morph.setInterpolator(down
                ? new PathInterpolator(0.2f, 0f, 0f, 1f)
                : new PathInterpolator(0.34f, 1.32f, 0.64f, 1f));
        morph.addUpdateListener(a -> {
            morphProgress = (float) a.getAnimatedValue();
            ShapeAppearanceModel shape = shapeFor(morphProgress);
            fill.setShapeAppearanceModel(shape);
            mask.setShapeAppearanceModel(shape);
        });
        morph.start();
    }

    /** Interpolates the resting bubble into the pressed shape; the tail corner stays put. */
    private ShapeAppearanceModel shapeFor(float progress) {
        float corner = radius + (dp(PRESSED_RADIUS_DP) - radius) * progress;
        ShapeAppearanceModel.Builder builder = ShapeAppearanceModel.builder()
                .setAllCornerSizes(corner);
        if (sharpCorner == SHARP_BOTTOM_RIGHT) {
            builder.setBottomRightCornerSize(tail);
        } else if (sharpCorner == SHARP_BOTTOM_LEFT) {
            builder.setBottomLeftCornerSize(tail);
        }
        return builder.build();
    }

    /**
     * A pressed state layer instead of a flat colour: the container deepens by 10% while the finger
     * is down, exactly like a filled tonal button.
     */
    private ColorStateList containerState(@ColorInt int base) {
        int onSurface = MaterialColors.getColor(this,
                com.google.android.material.R.attr.colorOnSurface);
        return new ColorStateList(
                new int[][]{
                        new int[]{-android.R.attr.state_enabled},
                        new int[]{android.R.attr.state_pressed},
                        new int[]{}
                },
                new int[]{
                        ColorUtils.setAlphaComponent(onSurface, 0x1F),
                        ColorUtils.blendARGB(base, onSurface, 0.10f),
                        base
                });
    }

    private ColorStateList rippleState() {
        int onSurface = MaterialColors.getColor(this,
                com.google.android.material.R.attr.colorOnSurface);
        return ColorStateList.valueOf(ColorUtils.setAlphaComponent(onSurface, 0x1F));
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
