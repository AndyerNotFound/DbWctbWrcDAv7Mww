package dev.neko.chatdemo.ui;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.google.android.material.color.MaterialColors;

import dev.neko.chatdemo.R;

/**
 * The action card of a message: a floating surface anchored under the bubble the finger held.
 *
 * <p>Each option is a filled rounded row that carries only an icon and a label — no icon container,
 * no outline, no trailing chevron. The rows are stacked with a small gap so they separate by shape
 * instead of by dividers, and the destructive option switches to the error container.
 */
public final class MessageActionsSheet {

    /** What the card reports back; every entry is one row. */
    public interface Callbacks {
        void onEdit();

        void onCopy();

        void onQuote();

        void onBranch();

        void onShare();

        void onDelete();
    }

    private static final int WIDTH_DP = 272;

    public static void show(@NonNull Activity activity, @NonNull View anchor,
                            @NonNull Callbacks callbacks) {
        FrameLayout host = new FrameLayout(activity);
        View content = LayoutInflater.from(activity)
                .inflate(R.layout.view_message_actions, host, false);
        LinearLayout list = content.findViewById(R.id.message_actions_content);

        PopupWindow popup = new PopupWindow(content,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popup.setOutsideTouchable(true);
        popup.setElevation(dp(content, 8));

        addRow(activity, list, popup, R.drawable.ic_content_copy,
                R.string.message_action_copy, false, callbacks::onCopy);
        addRow(activity, list, popup, R.drawable.ic_open_in_full,
                R.string.message_action_preview, false, callbacks::onQuote);
        addRow(activity, list, popup, R.drawable.ic_edit_note,
                R.string.message_action_edit, false, callbacks::onEdit);
        addRow(activity, list, popup, R.drawable.ic_north_east,
                R.string.message_action_share, false, callbacks::onShare);
        addRow(activity, list, popup, R.drawable.ic_alt_route,
                R.string.message_action_branch, false, callbacks::onBranch);
        addRow(activity, list, popup, R.drawable.ic_delete,
                R.string.message_action_delete, true, callbacks::onDelete);

        // Measure once so the card can be placed against the bubble before it is shown.
        int width = dp(content, WIDTH_DP);
        content.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        int cardWidth = content.getMeasuredWidth();
        int cardHeight = content.getMeasuredHeight();

        int[] location = new int[2];
        anchor.getLocationOnScreen(location);
        int screenWidth = activity.getResources().getDisplayMetrics().widthPixels;
        int screenHeight = activity.getResources().getDisplayMetrics().heightPixels;
        int margin = dp(content, 12);

        // End aligned with the bubble, then pulled back inside the screen.
        int x = location[0] + anchor.getWidth() - cardWidth;
        x = Math.max(margin, Math.min(x, screenWidth - cardWidth - margin));

        // Below the bubble when it fits, otherwise above it.
        int below = location[1] + anchor.getHeight() + dp(content, 4);
        int y = below + cardHeight + margin <= screenHeight
                ? below
                : Math.max(margin, location[1] - cardHeight - dp(content, 4));

        popup.showAtLocation(anchor, Gravity.NO_GRAVITY, x, y);

        // The card grows out of the bubble instead of blinking into place.
        content.setAlpha(0f);
        content.setScaleX(0.94f);
        content.setScaleY(0.94f);
        content.setPivotX(cardWidth);
        content.setPivotY(0f);
        content.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(170)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private static void addRow(Activity activity, LinearLayout list, PopupWindow popup,
                               @DrawableRes int icon, @StringRes int label, boolean danger,
                               Runnable action) {
        View row = LayoutInflater.from(activity)
                .inflate(R.layout.item_message_action, list, false);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        if (list.getChildCount() > 0) {
            params.topMargin = dp(row, 4);
        }
        row.setLayoutParams(params);

        ImageView iconView = row.findViewById(R.id.action_icon);
        TextView labelView = row.findViewById(R.id.action_label);
        iconView.setImageResource(icon);
        labelView.setText(label);

        int contentColor = MaterialColors.getColor(row, danger
                ? com.google.android.material.R.attr.colorOnErrorContainer
                : com.google.android.material.R.attr.colorOnSurfaceVariant);
        iconView.setImageTintList(ColorStateList.valueOf(contentColor));
        if (danger) {
            row.setBackgroundResource(R.drawable.bg_action_row_danger);
            labelView.setTextColor(MaterialColors.getColor(row,
                    com.google.android.material.R.attr.colorOnErrorContainer));
        }

        row.setOnClickListener(v -> {
            popup.dismiss();
            action.run();
        });
        list.addView(row);
    }

    private static int dp(View view, int value) {
        return Math.round(value * view.getResources().getDisplayMetrics().density);
    }

    private MessageActionsSheet() {
    }
}
