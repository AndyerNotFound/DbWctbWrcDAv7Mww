package dev.neko.chatdemo.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.RippleDrawable;
import android.util.AttributeSet;
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
 * the same filled container, the same 10% state layer while pressed, the same bounded ripple, and
 * the same accessibility role ({@code Button}) — while staying a layout, so a message can hold any
 * rich media inside it.
 *
 * <p>One corner is left nearly square so the bubble points at the sender. That is the only thing the
 * XML has to decide:
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

    private final MaterialShapeDrawable fill;

    public BubbleButton(Context context) {
        this(context, null);
    }

    public BubbleButton(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BubbleButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(VERTICAL);

        TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.BubbleButton, defStyleAttr, 0);
        float radius = a.getDimension(R.styleable.BubbleButton_bubbleCornerSize, dp(20));
        float tail = a.getDimension(R.styleable.BubbleButton_bubbleTailSize, dp(5));
        int sharp = a.getInt(R.styleable.BubbleButton_bubbleSharpCorner, SHARP_BOTTOM_RIGHT);
        int container = a.getColor(R.styleable.BubbleButton_bubbleContainerColor,
                MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimaryContainer));
        a.recycle();

        ShapeAppearanceModel.Builder builder = ShapeAppearanceModel.builder()
                .setAllCornerSizes(radius);
        if (sharp == SHARP_BOTTOM_RIGHT) {
            builder.setBottomRightCornerSize(tail);
        } else if (sharp == SHARP_BOTTOM_LEFT) {
            builder.setBottomLeftCornerSize(tail);
        }
        ShapeAppearanceModel shape = builder.build();

        fill = new MaterialShapeDrawable(shape);
        fill.setFillColor(containerState(container));
        fill.setElevation(0f);

        // The mask only needs the outline: the ripple is clipped to it, so the press feedback never
        // spills into the square corners the bubble leaves behind.
        MaterialShapeDrawable mask = new MaterialShapeDrawable(shape);
        mask.setFillColor(ColorStateList.valueOf(Color.WHITE));

        RippleDrawable ripple = new RippleDrawable(rippleState(), fill, mask);
        setBackground(ripple);
        setClickable(true);
        setFocusable(true);
    }

    /** The container colour, so callers can derive matching content colours from the same source. */
    @ColorInt
    public int containerColor() {
        ColorStateList list = fill.getFillColor();
        return list == null ? Color.TRANSPARENT : list.getDefaultColor();
    }

    /** Retints the bubble, e.g. while the message it belongs to is being edited. */
    public void setContainerColor(@ColorInt int color) {
        fill.setFillColor(containerState(color));
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return Button.class.getName();
    }

    /**
     * A pressed state layer instead of a flat colour: the container deepens by 10% while the finger
     * is down, exactly like a filled tonal button.
     */
    private ColorStateList containerState(@ColorInt int base) {
        int onSurface = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface);
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
        int onSurface = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface);
        return ColorStateList.valueOf(ColorUtils.setAlphaComponent(onSurface, 0x1F));
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
