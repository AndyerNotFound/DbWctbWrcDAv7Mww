package dev.neko.chatdemo.ui;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
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

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.listitem.ListItemLayout;
import com.google.android.material.materialswitch.MaterialSwitch;

import dev.neko.chatdemo.R;

/**
 * Settings rows built as a multi-section list: a section header names the block, and the rows
 * below it are segmented {@link ListItemLayout} items. Each row only knows its index inside its
 * own section, so the first row gets the large top corners, the last the large bottom corners and
 * the rows between stay square — the block reads as one continuous card.
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

    /** Gives the collapsing app bar the status bar height and the scroll view the nav bar height. */
    public static void applyInsets(View root, View appBar, View scroll) {
        final int top = appBar.getPaddingTop();
        final int bottom = scroll.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            appBar.setPadding(appBar.getPaddingLeft(), top + bars.top,
                    appBar.getPaddingRight(), appBar.getPaddingBottom());
            scroll.setPadding(scroll.getPaddingLeft(), scroll.getPaddingTop(),
                    scroll.getPaddingRight(), bottom + bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
    }

    /** Sets the page title on a collapsing app bar. */
    public static void setTitle(CollapsingToolbarLayout collapsing, CharSequence title) {
        collapsing.setTitle(title);
    }

    /** Adds a titled section to {@code parent} and returns the container rows go into. */
    public static LinearLayout group(Activity activity, LinearLayout parent, int titleRes) {
        return group(activity, parent, activity.getString(titleRes));
    }

    /** Same as {@link #group(Activity, LinearLayout, int)} but without a heading. */
    public static LinearLayout group(Activity activity, LinearLayout parent, CharSequence titleText) {
        LinearLayout section = new LinearLayout(activity);
        section.setOrientation(LinearLayout.VERTICAL);

        if (titleText != null && titleText.length() > 0) {
            TextView header = (TextView) LayoutInflater.from(activity)
                    .inflate(R.layout.item_list_subheader, section, false);
            header.setText(titleText);
            section.addView(header);
        }

        LinearLayout rows = new LinearLayout(activity);
        rows.setOrientation(LinearLayout.VERTICAL);
        section.addView(rows);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(activity.getWindow().getDecorView(),
                parent.getChildCount() == 0 ? 0 : 4);
        parent.addView(section, lp);
        return rows;
    }

    /**
     * Adds one segmented row. The returned view is the clickable card, so callers can hang a click
     * listener on it directly; the corners are resolved later by {@link #finishGroup}.
     */
    public static View row(Activity activity, LinearLayout group, int iconRes,
                           CharSequence title, CharSequence subtitle,
                           int containerColor, int contentColor) {
        ListItemLayout item = (ListItemLayout) LayoutInflater.from(activity)
                .inflate(R.layout.item_pref_row, group, false);
        bindRow(item, iconRes, title, subtitle, containerColor, contentColor);
        group.addView(item);
        return item.findViewById(R.id.pref_card);
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

    /** Applies the section position state to every row so the block closes on rounded corners. */
    public static void finishGroup(LinearLayout group) {
        int count = group.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = group.getChildAt(i);
            if (!(child instanceof ListItemLayout)) {
                continue;
            }
            ((ListItemLayout) child).updateAppearance(i, count);
            // Segments sit 2dp apart; the block's own bottom edge stays flush so the rounded
            // last row ends the card cleanly.
            ViewGroup.LayoutParams params = child.getLayoutParams();
            if (params instanceof ViewGroup.MarginLayoutParams) {
                ((ViewGroup.MarginLayoutParams) params).bottomMargin =
                        dp(child, i == count - 1 ? 0 : 2);
                child.setLayoutParams(params);
            }
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
