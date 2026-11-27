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
import androidx.transition.Fade;
import androidx.transition.TransitionManager;
import androidx.transition.TransitionSet;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;

import java.util.ArrayList;
import java.util.List;

import dev.neko.chatdemo.R;

/**
 * One card for everything the assistant did before it spoke: the reasoning pass and the tool calls.
 *
 * <p>Modelled on RikkaHub's chain of thought. The fold control is a single line pinned at the top of
 * the card and it never moves or disappears: folded it reads "show N more steps" with a chevron
 * pointing down, open it reads "collapse" with the chevron turned over. What folds away is the
 * <em>leading</em> steps — the tail always stays, so the last two steps, the ones that led to the
 * reply on screen, are what a folded card shows.
 *
 * <p>Each step is a row of its own: an icon, a two-line label, a trailing chevron when it opens
 * something, and a meter under it whose length is the step's share of the elapsed time with the
 * duration printed at the end.
 */
public class StepCardView extends MaterialCardView {

    /** How many trailing steps survive the fold. */
    public static final int VISIBLE_STEPS = 2;

    /** Tells the row that the fold state changed, so a rebind does not undo the user's choice. */
    public interface ExpandListener {
        void onExpandChanged(boolean expanded);
    }

    private final LinearLayout rows;
    private final LinearLayout toggle;
    private final TextView toggleLabel;
    private final ImageView toggleChevron;

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

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);

        // The fold control: chevron plus label, primary, no container of its own. It sits at the top
        // and stays put — only its text and chevron turn over with the state.
        toggle = new LinearLayout(context);
        toggle.setOrientation(LinearLayout.HORIZONTAL);
        toggle.setGravity(Gravity.CENTER_VERTICAL);
        toggle.setMinimumHeight(dp(44));
        toggle.setPadding(dp(10), dp(4), dp(12), dp(4));
        toggle.setBackgroundResource(R.drawable.bg_step_toggle);
        toggle.setClickable(true);
        toggle.setFocusable(true);

        toggleChevron = new ImageView(context);
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
            // Height and opacity travel together, so a fold never snaps the card to its new size.
            TransitionSet set = new TransitionSet()
                    .addTransition(new ChangeBounds())
                    .addTransition(new Fade());
            set.setDuration(220);
            TransitionManager.beginDelayedTransition(this, set);
        }
        rows.removeAllViews();

        // The leading steps fold away; the tail — the ones that led to this reply — always stays.
        boolean canCollapse = steps.size() > VISIBLE_STEPS;
        int hidden = expanded || !canCollapse ? 0 : steps.size() - VISIBLE_STEPS;

        float longest = 0f;
        for (int i = hidden; i < steps.size(); i++) {
            longest = Math.max(longest, steps.get(i).progress);
        }
        for (int i = hidden; i < steps.size(); i++) {
            rows.addView(stepRow(steps.get(i), longest));
        }

        // One line, one place. It only exists when there is something to fold.
        toggle.setVisibility(canCollapse ? VISIBLE : GONE);
        if (canCollapse) {
            toggleLabel.setText(expanded
                    ? getContext().getString(R.string.steps_collapse)
                    : getContext().getString(R.string.steps_hidden, steps.size() - VISIBLE_STEPS));
            toggleChevron.animate().cancel();
            if (animate) {
                toggleChevron.animate()
                        .rotation(expanded ? 180f : 0f)
                        .setDuration(200)
                        .start();
            } else {
                toggleChevron.setRotation(expanded ? 180f : 0f);
            }
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

        meter.addView(track, new LinearLayout.LayoutParams(0, dp(3), 1f));

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
