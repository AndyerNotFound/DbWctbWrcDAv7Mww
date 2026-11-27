package dev.neko.chatdemo.ui;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.transition.ChangeBounds;
import androidx.transition.TransitionManager;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;

import dev.neko.chatdemo.R;

/**
 * Session drawer: collapsing title, a row of function buttons, then the chat history grouped by
 * time. Each history entry is a checkable button and the current session stays toggled.
 */
public class SidebarDrawer {

    public interface Callbacks {
        void onNewSession();

        void onSearch();

        void onFolders();

        void onSessionChosen(String title);

        void onOpenSettings(View fab);
    }

    private static final String[][] SESSIONS = {
            {"Late night refactor", "Sheet motion study"},
            {"Palette pass"},
            {"Apr 12 · build log"}
    };
    private static final int[] GROUP_TITLES = {
            R.string.group_today, R.string.group_yesterday, R.string.group_earlier
    };

    private final Activity activity;
    private final DrawerLayout drawer;
    private final Callbacks callbacks;
    private MaterialButton current;

    public SidebarDrawer(Activity activity, DrawerLayout drawer, Callbacks callbacks) {
        this.activity = activity;
        this.drawer = drawer;
        this.callbacks = callbacks;
    }

    public void setUp() {
        CollapsingToolbarLayout collapsing = activity.findViewById(R.id.drawer_collapsing);
        collapsing.setTitle(activity.getString(R.string.sidebar_title));

        MaterialToolbar toolbar = activity.findViewById(R.id.drawer_toolbar);
        toolbar.setNavigationOnClickListener(v -> close());

        activity.findViewById(R.id.drawer_function_new).setOnClickListener(v -> {
            close();
            callbacks.onNewSession();
        });
        activity.findViewById(R.id.drawer_function_search).setOnClickListener(v -> {
            close();
            callbacks.onSearch();
        });
        activity.findViewById(R.id.drawer_function_folders).setOnClickListener(v -> {
            close();
            callbacks.onFolders();
        });

        buildHistory(activity.findViewById(R.id.drawer_history));

        View fab = activity.findViewById(R.id.drawer_fab);
        fab.setOnClickListener(v -> callbacks.onOpenSettings(fab));
    }

    private void buildHistory(LinearLayout host) {
        int muted = MaterialColors.getColor(host,
                androidx.appcompat.R.attr.colorOnSurfaceVariant);

        TextView heading = new TextView(activity);
        heading.setText(R.string.sidebar_history);
        heading.setTextSize(12);
        heading.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        heading.setTextColor(muted);
        heading.setPadding(dp(4), dp(2), dp(4), dp(6));
        host.addView(heading);

        for (int group = 0; group < SESSIONS.length; group++) {
            LinearLayout header = new LinearLayout(activity);
            header.setOrientation(LinearLayout.HORIZONTAL);
            header.setGravity(Gravity.CENTER_VERTICAL);
            header.setPadding(dp(4), dp(8), dp(4), dp(8));
            header.setClickable(true);
            header.setFocusable(true);

            TextView label = new TextView(activity);
            label.setText(GROUP_TITLES[group]);
            label.setTextSize(12);
            label.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            label.setTextColor(muted);
            header.addView(label, new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            ImageView chevron = new ImageView(activity);
            chevron.setImageResource(R.drawable.ic_keyboard_arrow_down);
            chevron.setImageTintList(ColorStateList.valueOf(muted));
            header.addView(chevron, new LinearLayout.LayoutParams(dp(18), dp(18)));

            LinearLayout items = new LinearLayout(activity);
            items.setOrientation(LinearLayout.VERTICAL);

            for (String title : SESSIONS[group]) {
                MaterialButton button = (MaterialButton) LayoutInflater.from(activity)
                        .inflate(R.layout.item_drawer_session, items, false);
                button.setText(title);
                button.setCheckable(true);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(44));
                lp.topMargin = dp(4);
                items.addView(button, lp);
                button.setOnClickListener(v -> choose(button, title));
                if (current == null) {
                    button.setChecked(true);
                    current = button;
                }
            }

            host.addView(header);
            host.addView(items);

            header.setOnClickListener(v -> {
                boolean show = items.getVisibility() != View.VISIBLE;
                TransitionManager.beginDelayedTransition(host, new ChangeBounds().setDuration(200));
                items.setVisibility(show ? View.VISIBLE : View.GONE);
                chevron.animate().rotation(show ? 0f : -90f).setDuration(200).start();
            });
        }
    }

    private void choose(MaterialButton button, String title) {
        if (current != null && current != button) {
            current.setChecked(false);
        }
        button.setChecked(true);
        current = button;
        callbacks.onSessionChosen(title);
        close();
    }

    private void close() {
        drawer.closeDrawer(GravityCompat.START);
    }

    private int dp(int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
