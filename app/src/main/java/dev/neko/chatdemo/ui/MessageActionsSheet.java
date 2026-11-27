package dev.neko.chatdemo.ui;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;

import dev.neko.chatdemo.R;

/**
 * The action card of a message: a floating surface anchored under the bubble the finger held.
 *
 * <p>The card is a multi-section list — the same construction the settings pages use. Each block is
 * named by a section subheader and its options are {@link com.google.android.material.listitem
 * .ListItemLayout} segments, so the options inside one block share a single continuous card and
 * only the first and the last round their corners. Choosing a row closes the card before the action
 * runs, so the action can safely change the list the bubble belongs to.
 */
public final class MessageActionsSheet {

    /** What the card reports back; every entry is one row. */
    public interface Callbacks {
        void onEdit();

        void onCopy();

        void onQuote();

        void onBranch();

        void onReadAloud();

        void onDelete();
    }

    private static final int MAX_WIDTH_DP = 300;

    public static void show(@NonNull Activity activity, @NonNull View anchor,
                            @NonNull Callbacks callbacks) {
        FrameLayout host = new FrameLayout(activity);
        View content = LayoutInflater.from(activity)
                .inflate(R.layout.view_message_actions, host, false);
        LinearLayout list = content.findViewById(R.id.message_actions_content);

        int primary = SettingsUi.color(content,
                com.google.android.material.R.attr.colorPrimaryContainer);
        int onPrimary = SettingsUi.color(content,
                com.google.android.material.R.attr.colorOnPrimaryContainer);
        int error = SettingsUi.color(content,
                com.google.android.material.R.attr.colorErrorContainer);
        int onError = SettingsUi.color(content,
                com.google.android.material.R.attr.colorOnErrorContainer);

        PopupWindow popup = new PopupWindow(content,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popup.setOutsideTouchable(true);
        popup.setElevation(dp(content, 6));

        LinearLayout clipboard = SettingsUi.group(activity, list,
                activity.getString(R.string.message_action_copy_section));
        row(activity, clipboard, popup, R.drawable.ic_content_copy,
                R.string.message_action_copy, null, primary, onPrimary, callbacks::onCopy);
        row(activity, clipboard, popup, R.drawable.ic_chat_bubble,
                R.string.message_action_quote, null, primary, onPrimary, callbacks::onQuote);
        SettingsUi.finishGroup(clipboard);

        LinearLayout thread = SettingsUi.group(activity, list,
                activity.getString(R.string.message_action_thread_section));
        row(activity, thread, popup, R.drawable.ic_edit_note,
                R.string.message_action_edit, null, primary, onPrimary, callbacks::onEdit);
        row(activity, thread, popup, R.drawable.ic_alt_route,
                R.string.message_action_branch, null, primary, onPrimary, callbacks::onBranch);
        row(activity, thread, popup, R.drawable.ic_graphic_eq,
                R.string.message_action_speak, null, primary, onPrimary, callbacks::onReadAloud);
        SettingsUi.finishGroup(thread);

        LinearLayout danger = SettingsUi.group(activity, list, (CharSequence) null);
        row(activity, danger, popup, R.drawable.ic_delete,
                R.string.message_action_delete, R.string.message_action_delete_sub,
                error, onError, callbacks::onDelete);
        SettingsUi.finishGroup(danger);

        // Measure once so the card can be placed against the bubble before it is shown.
        int width = dp(content, MAX_WIDTH_DP);
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
        content.setScaleX(0.92f);
        content.setScaleY(0.92f);
        content.setPivotX(cardWidth);
        content.setPivotY(0f);
        content.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(160)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private static void row(Activity activity, LinearLayout group, PopupWindow popup, int icon,
                            int title, Integer subtitle, int container, int content,
                            Runnable action) {
        View row = SettingsUi.row(activity, group, icon,
                activity.getString(title),
                subtitle == null ? null : activity.getString(subtitle),
                container, content);
        // Action rows do not open anything else, so no trailing chevron.
        View chevron = row.findViewById(R.id.pref_chevron);
        if (chevron != null) {
            chevron.setVisibility(View.GONE);
        }
        row.setOnClickListener(v -> {
            popup.dismiss();
            action.run();
        });
    }

    private static int dp(View view, int value) {
        return Math.round(value * view.getResources().getDisplayMetrics().density);
    }

    private MessageActionsSheet() {
    }
}
