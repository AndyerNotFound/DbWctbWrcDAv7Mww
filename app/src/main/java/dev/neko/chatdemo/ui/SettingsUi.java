package dev.neko.chatdemo.ui;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.materialswitch.MaterialSwitch;

import dev.neko.chatdemo.R;

/**
 * Settings rows built as preference cards: one card per section, plain rows inside separated by a
 * 1dp divider, a 40dp icon well and a title / subtitle column.
 */
public final class SettingsUi {

    public static void setupWindow(AppCompatActivity activity) {
        WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);
        activity.getWindow().setStatusBarColor(Color.TRANSPARENT);
        activity.getWindow().setNavigationBarColor(Color.TRANSPARENT);
        boolean night = (activity.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(activity.getWindow(), activity.getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(!night);
        controller.setAppearanceLightNavigationBars(!night);
    }

    public static void applyInsets(View root, View topBar, View scroll) {
        final int sidePadding = dp(topBar, 8);
        final int bottomPadding = scroll.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            topBar.setMinimumHeight(dp(topBar, 56) + bars.top);
            topBar.setPadding(bars.left + sidePadding, bars.top, bars.right + sidePadding, 0);
            scroll.setPadding(scroll.getPaddingLeft(), scroll.getPaddingTop(),
                    scroll.getPaddingRight(), bottomPadding + bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
    }

    /** Adds a titled card to {@code parent} and returns the container rows go into. */
    public static LinearLayout group(Activity activity, LinearLayout parent, int titleRes) {
        return group(activity, parent, activity.getString(titleRes));
    }

    /** Same as {@link #group(Activity, LinearLayout, int)} but without a heading. */
    public static LinearLayout group(Activity activity, LinearLayout parent, CharSequence titleText) {
        LinearLayout block = new LinearLayout(activity);
        block.setOrientation(LinearLayout.VERTICAL);

        if (titleText != null && titleText.length() > 0) {
            TextView title = new TextView(activity);
            title.setText(titleText);
            title.setTextSize(14);
            title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            title.setTextColor(MaterialColors.getColor(title,
                    com.google.android.material.R.attr.colorOnSurfaceVariant));
            title.setPadding(dp(title, 4), dp(title, 8), dp(title, 4), dp(title, 8));
            block.addView(title);
        }

        MaterialCardView card = new MaterialCardView(activity);
        card.setCardBackgroundColor(MaterialColors.getColor(card,
                com.google.android.material.R.attr.colorSurfaceContainerLow));
        card.setRadius(dp(card, 20));
        card.setCardElevation(0);
        card.setUseCompatPadding(false);
        card.setClipToOutline(true);

        LinearLayout rows = new LinearLayout(activity);
        rows.setOrientation(LinearLayout.VERTICAL);
        card.addView(rows, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        block.addView(card);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(activity.getWindow().getDecorView(),
                parent.getChildCount() == 0 ? 0 : 12);
        parent.addView(block, lp);
        return rows;
    }

    public static View row(Activity activity, LinearLayout group, int iconRes,
                           CharSequence title, CharSequence subtitle,
                           int containerColor, int contentColor) {
        View row = LayoutInflater.from(activity).inflate(R.layout.item_pref_row, group, false);
        bindRow(row, iconRes, title, subtitle, containerColor, contentColor);
        group.addView(row);
        return row;
    }

    public static View switchRow(Activity activity, LinearLayout group, int iconRes,
                                 CharSequence title, boolean checked,
                                 int containerColor, int contentColor) {
        View row = row(activity, group, iconRes, title, null, containerColor, contentColor);
        row.findViewById(R.id.pref_chevron).setVisibility(View.GONE);
        MaterialSwitch toggle = row.findViewById(R.id.pref_switch);
        toggle.setVisibility(View.VISIBLE);
        toggle.setChecked(checked);
        row.setOnClickListener(v -> toggle.setChecked(!toggle.isChecked()));
        return row;
    }

    /** Shows the value capsule at the end of a row. */
    public static void setValue(View row, CharSequence value) {
        TextView pill = row.findViewById(R.id.pref_value);
        pill.setText(value);
        pill.setVisibility(View.VISIBLE);
    }

    /** Hides the divider of the last row so the card closes cleanly. */
    public static void finishGroup(LinearLayout group) {
        if (group.getChildCount() == 0) {
            return;
        }
        View divider = group.getChildAt(group.getChildCount() - 1).findViewById(R.id.pref_divider);
        if (divider != null) {
            divider.setVisibility(View.GONE);
        }
    }

    private static void bindRow(View row, int iconRes, CharSequence title, CharSequence subtitle,
                                int containerColor, int contentColor) {
        View iconBg = row.findViewById(R.id.pref_icon_bg);
        iconBg.setBackgroundTintList(ColorStateList.valueOf(containerColor));
        ImageView icon = row.findViewById(R.id.pref_icon);
        icon.setImageResource(iconRes);
        icon.setImageTintList(ColorStateList.valueOf(contentColor));

        ((TextView) row.findViewById(R.id.pref_title)).setText(title);
        TextView sub = row.findViewById(R.id.pref_subtitle);
        if (subtitle == null || subtitle.length() == 0) {
            sub.setVisibility(View.GONE);
        } else {
            sub.setText(subtitle);
        }
    }

    public static int color(View view, int attrRes) {
        return MaterialColors.getColor(view, attrRes);
    }

    public static int dp(View view, int value) {
        return Math.round(value * view.getResources().getDisplayMetrics().density);
    }

    private SettingsUi() {
    }
}
