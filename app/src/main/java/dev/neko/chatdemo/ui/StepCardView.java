package dev.neko.chatdemo.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.transition.ChangeBounds;
import androidx.transition.TransitionManager;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;

import java.util.ArrayList;
import java.util.List;

import dev.neko.chatdemo.R;

/**
 * One card for everything the assistant did before it spoke: the reasoning pass and the tool calls.
 *
 * <p>The fold is a plain chevron-and-label line in the primary colour, with no container of its own.
 * Collapsed, the card keeps the latest {@value #VISIBLE_STEPS} steps — the ones that led to the
 * reply on screen — and its first line offers "show N more steps". Tapping it lists every step and
 * the line steps aside; folding back is the caller's line above the card.
 *
 * <p>Each step is a row of its own: an icon, the label (two lines at most), a trailing chevron when
 * there is more to open, and a meter under it — a thin bar whose length is the step's share of the
 * time spent, with the duration printed at its end.
 */
public class StepCardView extends MaterialCardView {

    /** How many steps survive the collapse. */
    public static final int VISIBLE_STEPS = 2;

    /** Tells the row that the fold state changed, so a rebind does not undo the user's choice. */
    public interface ExpandListener {
        void onExpandChanged(boolean expanded);
    }

    private final LinearLayout rows;
    private final LinearLayout toggle;
    private final TextView toggleLabel;

    private final List<ChatItem.Step> steps = new ArrayList<>();
    private boolean expanded;

    @Nullable
    private ExpandListener expandListener;

    public StepCardView(@NonNull Context context) {
        this(context, null);
    }

    public StepCardView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StepCardView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        int primary = MaterialColors.getColor(this, androidx.appcompat.R.attr.colorPrimary);
        int onSurface = MaterialColors.getColor(this,
                com.google.android.material.R.attr.colorOnSurface);
        int variant = MaterialColors.getColor(this,
                com.google.android.material.R.attr.colorOnSurfaceVariant);

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);

        // The fold line: chevron plus label, primary, no container of its own.
        toggle = new LinearLayout(context);
        toggle.setOrientation(LinearLayout.HORIZONTAL);
        toggle.setGravity(Gravity.CENTER_VERTICAL);
        toggle.setMinimumHeight(dp(44));
        toggle.setPadding(dp(12), dp(6), dp(12), dp(6));
        toggle.setBackgroundResource(R.drawable.bg_step_toggle);
        toggle.setClickable(true);
        toggle.setFocusable(true);

        ImageView toggleChevron = new ImageView(context);
        toggleChevron.setImageResource(R.drawable.ic_expand_more);
        toggleChevron.setImageTintList(ColorStateList.valueOf(primary));
        toggle.addView(toggleChevron, new LinearLayout.LayoutParams(dp(20), dp(20)));

        toggleLabel = new TextView(context);
        toggleLabel.setTextAppearance(
                com.google.android.material.R.style.TextAppearance_Material3_LabelLarge);
        toggleLabel.setTextColor(primary);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        labelParams.setMarginStart(dp(8));
        toggle.addView(toggleLabel, labelParams);

        toggle.setOnClickListener(v -> setExpanded(!expanded, true));
        content.addView(toggle);

        rows = new LinearLayout(context);
        rows.setOrientation(LinearLayout.VERTICAL);
        rows.setPadding(dp(12), 0, dp(12), dp(6));
        content.addView(rows);

        addView(content);
        setCardBackgroundColor(MaterialColors.getColor(this,
                com.google.android.material.R.attr.colorSurfaceContainerLow));
        setCardElevation(0f);
        setRadius(dp(20));
    }

    /** Fills the card. An empty list hides it, so a plain reply keeps no empty shell. */
    public void bind(@NonNull List<ChatItem.Step> source, boolean expanded) {
        steps.clear();
        steps.addAll(source);
        this.expanded = expanded;
        if (steps.isEmpty()) {
            setVisibility(GONE);
            return;
        }
        setVisibility(VISIBLE);
        render(false);
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpandListener(@Nullable ExpandListener listener) {
        this.expandListener = listener;
    }

    public void setExpanded(boolean value, boolean animate) {
        if (expanded == value) {
            return;
        }
        expanded = value;
        render(animate);
        if (expandListener != null) {
            expandListener.onExpandChanged(expanded);
        }
    }

    private void render(boolean animate) {
        if (animate) {
            // The card grows and shrinks by its own height instead of jumping between two sizes.
            TransitionManager.beginDelayedTransition(this, new ChangeBounds());
        }
        rows.removeAllViews();

        int hidden = expanded ? 0 : Math.max(0, steps.size() - VISIBLE_STEPS);
        float longest = 0f;
        for (int i = hidden; i < steps.size(); i++) {
            longest = Math.max(longest, steps.get(i).progress);
        }
        for (int i = hidden; i < steps.size(); i++) {
            rows.addView(stepRow(steps.get(i), longest));
        }

        // The inner line only offers the steps that are folded away; once the card is open the
        // outer line above the card is the way back, so this one steps aside.
        if (hidden > 0) {
            toggle.setVisibility(VISIBLE);
            toggleLabel.setText(getContext().getString(R.string.steps_hidden, hidden));
        } else {
            toggle.setVisibility(GONE);
        }
    }

    /**
     * One step: icon and label on top with a trailing chevron, then a meter whose fill is this
     * step's share of the longest step on screen, with the duration at the end of the bar.
     */
    private View stepRow(ChatItem.Step step, float longest) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, dp(9), 0, dp(9));

        LinearLayout top = new LinearLayout(getContext());
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.TOP);

        ImageView icon = new ImageView(getContext());
        icon.setImageResource(step.icon);
        icon.setImageTintList(ColorStateList.valueOf(MaterialColors.getColor(this,
                com.google.android.material.R.attr.colorOnSurfaceVariant)));
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(18), dp(18));
        iconParams.topMargin = dp(1);
        top.addView(icon, iconParams);

        TextView title = new TextView(getContext());
        title.setText(step.title);
        title.setTextAppearance(
                com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
        title.setTextColor(MaterialColors.getColor(this,
                com.google.android.material.R.attr.colorOnSurface));
        title.setMaxLines(2);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.setMarginStart(dp(12));
        top.addView(title, titleParams);

        if (step.expandable) {
            ImageView chevron = new ImageView(getContext());
            chevron.setImageResource(R.drawable.ic_chevron_right);
            chevron.setImageTintList(ColorStateList.valueOf(MaterialColors.getColor(this,
                    com.google.android.material.R.attr.colorOnSurfaceVariant)));
            LinearLayout.LayoutParams chevronParams =
                    new LinearLayout.LayoutParams(dp(18), dp(18));
            chevronParams.setMarginStart(dp(6));
            chevronParams.topMargin = dp(1);
            top.addView(chevron, chevronParams);
        }

        row.addView(top);

        LinearLayout meter = new LinearLayout(getContext());
        meter.setOrientation(LinearLayout.HORIZONTAL);
        meter.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams meterParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        meterParams.topMargin = dp(9);
        meterParams.setMarginStart(dp(30));

        LinearLayout track = new LinearLayout(getContext());
        track.setOrientation(LinearLayout.HORIZONTAL);
        track.setBackgroundResource(R.drawable.bg_step_meter_track);

        float share = longest <= 0f ? 1f : Math.max(0.04f, step.progress / longest);
        View filled = new View(getContext());
        filled.setBackgroundResource(R.drawable.bg_step_meter_fill);
        track.addView(filled, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.MATCH_PARENT, share));
        track.addView(new View(getContext()), new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.MATCH_PARENT, Math.max(0.0001f, 1f - share)));

        LinearLayout.LayoutParams trackParams = new LinearLayout.LayoutParams(
                0, dp(3), 1f);
        meter.addView(track, trackParams);

        TextView duration = new TextView(getContext());
        duration.setText(step.meta);
        duration.setTextAppearance(
                com.google.android.material.R.style.TextAppearance_Material3_LabelSmall);
        duration.setTextColor(MaterialColors.getColor(this,
                com.google.android.material.R.attr.colorOnSurfaceVariant));
        LinearLayout.LayoutParams durationParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        durationParams.setMarginStart(dp(10));
        meter.addView(duration, durationParams);

        row.addView(meter);
        return row;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
