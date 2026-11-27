package dev.neko.chatdemo.ui;

import android.content.Context;
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
 * <p>Collapsed, the card keeps only the latest {@value #VISIBLE_STEPS} steps — the ones that led to
 * the reply on screen — and folds the rest behind its header. Expanded, every step is listed. The
 * header is the toggle; it carries the step count and a chevron that turns over with the state.
 */
public class StepCardView extends MaterialCardView {

    /** How many steps survive the collapse. */
    public static final int VISIBLE_STEPS = 2;

    /** Tells the row that the fold state changed, so a rebind does not undo the user's choice. */
    public interface ExpandListener {
        void onExpandChanged(boolean expanded);
    }

    private final LinearLayout rows;
    private final TextView count;
    private final ImageView chevron;

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

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);

        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setMinimumHeight(dp(44));
        header.setPadding(dp(12), 0, dp(10), 0);
        header.setBackgroundResource(R.drawable.bg_step_header);
        header.setClickable(true);
        header.setFocusable(true);

        ImageView icon = new ImageView(context);
        icon.setImageResource(R.drawable.ic_psychology);
        icon.setImageTintList(android.content.res.ColorStateList.valueOf(
                MaterialColors.getColor(this, androidx.appcompat.R.attr.colorPrimary)));
        header.addView(icon, new LinearLayout.LayoutParams(dp(17), dp(17)));

        TextView title = new TextView(context);
        title.setText(R.string.steps_title);
        title.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelLarge);
        title.setTextColor(MaterialColors.getColor(this,
                com.google.android.material.R.attr.colorOnSurface));
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.setMarginStart(dp(8));
        header.addView(title, titleParams);

        count = new TextView(context);
        count.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelSmall);
        count.setTextColor(MaterialColors.getColor(this,
                com.google.android.material.R.attr.colorOnSurfaceVariant));
        header.addView(count);

        chevron = new ImageView(context);
        chevron.setImageResource(R.drawable.ic_expand_more);
        chevron.setImageTintList(android.content.res.ColorStateList.valueOf(
                MaterialColors.getColor(this,
                        com.google.android.material.R.attr.colorOnSurfaceVariant)));
        LinearLayout.LayoutParams chevronParams = new LinearLayout.LayoutParams(dp(18), dp(18));
        chevronParams.setMarginStart(dp(4));
        header.addView(chevron, chevronParams);

        content.addView(header);

        rows = new LinearLayout(context);
        rows.setOrientation(LinearLayout.VERTICAL);
        rows.setPadding(dp(12), 0, dp(12), dp(10));
        content.addView(rows);

        header.setOnClickListener(v -> setExpanded(!expanded, true));

        addView(content);
        setCardBackgroundColor(MaterialColors.getColor(this,
                com.google.android.material.R.attr.colorSurfaceContainerLow));
        setCardElevation(0f);
        setRadius(dp(16));
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
        for (int i = hidden; i < steps.size(); i++) {
            rows.addView(stepRow(steps.get(i)));
        }

        int total = steps.size();
        count.setText(expanded
                ? getContext().getString(R.string.steps_count, total)
                : hidden > 0
                ? getContext().getString(R.string.steps_hidden, hidden)
                : getContext().getString(R.string.steps_count, total));

        chevron.animate().cancel();
        if (animate) {
            chevron.animate()
                    .rotation(expanded ? 180f : 0f)
                    .setDuration(180)
                    .start();
        } else {
            chevron.setRotation(expanded ? 180f : 0f);
        }
    }

    private View stepRow(ChatItem.Step step) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.TOP);
        row.setPadding(0, dp(7), 0, dp(7));

        ImageView icon = new ImageView(getContext());
        icon.setImageResource(step.icon);
        icon.setImageTintList(android.content.res.ColorStateList.valueOf(
                MaterialColors.getColor(this,
                        com.google.android.material.R.attr.colorOnSurfaceVariant)));
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(15), dp(15));
        iconParams.topMargin = dp(2);
        row.addView(icon, iconParams);

        LinearLayout text = new LinearLayout(getContext());
        text.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        textParams.setMarginStart(dp(9));

        TextView title = new TextView(getContext());
        title.setText(step.title);
        title.setTextAppearance(
                com.google.android.material.R.style.TextAppearance_Material3_BodySmall);
        title.setTextColor(MaterialColors.getColor(this,
                com.google.android.material.R.attr.colorOnSurface));
        text.addView(title);

        if (step.meta != null && step.meta.length() > 0) {
            TextView meta = new TextView(getContext());
            meta.setText(step.meta);
            meta.setTextAppearance(
                    com.google.android.material.R.style.TextAppearance_Material3_LabelSmall);
            meta.setTextColor(MaterialColors.getColor(this,
                    com.google.android.material.R.attr.colorOnSurfaceVariant));
            meta.setPadding(0, dp(1), 0, 0);
            text.addView(meta);
        }

        row.addView(text, textParams);
        return row;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
