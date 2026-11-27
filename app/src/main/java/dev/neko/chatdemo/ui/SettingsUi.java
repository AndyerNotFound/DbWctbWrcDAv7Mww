package dev.neko.chatdemo.ui;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
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
 * Card group building blocks for the settings pages.
 *
 * <p>A group is a title plus a stack of rows with a 2dp gap; the first and last row get 20dp
 * corners, the ones between them 4dp. That is the CardGroup geometry the settings reference uses,
 * so the whole app has one row style instead of one card per row.
 */
public final class SettingsUi {

    private static final int CORNER_EDGE = 20;
    private static final int CORNER_MIDDLE = 4;
    private static final int GAP = 2;

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

    /** Adds a titled group to {@code parent} and returns the container rows go into. */
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
            title.setTextColor(MaterialColors.getColor(title, androidx.appcompat.R.attr.colorPrimary));
            title.setPadding(dp(title, 4), dp(title, 8), dp(title, 4), dp(title, 8));
            block.addView(title);
        }

        LinearLayout rows = new LinearLayout(activity);
        rows.setOrientation(LinearLayout.VERTICAL);
        block.addView(rows);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(activity.getWindow().getDecorView(), parent.getChildCount() == 0 ? 0 : 12);
        parent.addView(block, lp);
        return rows;
    }

    public static MaterialCardView row(Activity activity, LinearLayout group, int iconRes,
                                       CharSequence title, CharSequence subtitle,
                                       int containerColor, int contentColor) {
        LayoutInflater inflater = LayoutInflater.from(activity);
        MaterialCardView card = (MaterialCardView) inflater.inflate(R.layout.item_pref_row, group, false);

        View iconBg = card.findViewById(R.id.pref_icon_bg);
        iconBg.setBackgroundTintList(android.content.res.ColorStateList.valueOf(containerColor));
        ImageView icon = card.findViewById(R.id.pref_icon);
        icon.setImageResource(iconRes);
        icon.setImageTintList(android.content.res.ColorStateList.valueOf(contentColor));

        ((TextView) card.findViewById(R.id.pref_title)).setText(title);
        TextView sub = card.findViewById(R.id.pref_subtitle);
        if (subtitle == null || subtitle.length() == 0) {
            sub.setVisibility(View.GONE);
        } else {
            sub.setText(subtitle);
        }

        group.addView(card);
        return card;
    }

    public static MaterialCardView switchRow(Activity activity, LinearLayout group, int iconRes,
                                             CharSequence title, boolean checked,
                                             int containerColor, int contentColor) {
        MaterialCardView card = row(activity, group, iconRes, title, null, containerColor, contentColor);
        card.findViewById(R.id.pref_chevron).setVisibility(View.GONE);
        MaterialSwitch toggle = card.findViewById(R.id.pref_switch);
        toggle.setVisibility(View.VISIBLE);
        toggle.setChecked(checked);
        card.setOnClickListener(v -> toggle.setChecked(!toggle.isChecked()));
        return card;
    }

    /** Applies the 2dp gap and the edge/middle corner radii to every row of a group. */
    public static void layoutGroup(LinearLayout group) {
        int count = group.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = group.getChildAt(i);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = i == 0 ? 0 : dp(child, GAP);
            child.setLayoutParams(lp);
            if (child instanceof MaterialCardView) {
                MaterialCardView card = (MaterialCardView) child;
                int top = i == 0 ? CORNER_EDGE : CORNER_MIDDLE;
                int bottom = i == count - 1 ? CORNER_EDGE : CORNER_MIDDLE;
                card.setShapeAppearanceModel(card.getShapeAppearanceModel().toBuilder()
                        .setTopLeftCornerSize(dp(child, top))
                        .setTopRightCornerSize(dp(child, top))
                        .setBottomLeftCornerSize(dp(child, bottom))
                        .setBottomRightCornerSize(dp(child, bottom))
                        .build());
            }
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
