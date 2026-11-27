package dev.neko.chatdemo;

import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;
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
import androidx.core.view.WindowInsetsCompat;
import androidx.transition.ChangeBounds;
import androidx.transition.TransitionManager;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.snackbar.Snackbar;

import dev.neko.chatdemo.ui.SettingsUi;
import dev.neko.chatdemo.ui.UiTransitions;

/** Full page session browser: collapsing title, functions, then the history grouped by day. */
public class SidebarActivity extends AppCompatActivity {

    private static final String[][] SESSIONS = {
            {"Late night refactor", "Sheet motion study"},
            {"Palette pass"},
            {"Apr 12 · build log"}
    };
    private static final int[] GROUP_TITLES = {
            R.string.group_today, R.string.group_yesterday, R.string.group_earlier
    };

    private MaterialButton current;
    private View root;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SettingsUi.setupWindow(this);
        UiTransitions.incomingPanel(this);
        setContentView(R.layout.activity_sidebar);
        root = findViewById(R.id.sidebar_root);

        View appBar = findViewById(R.id.sidebar_app_bar);
        View scroll = findViewById(R.id.sidebar_scroll);
        View fab = findViewById(R.id.sidebar_fab);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            appBar.setPadding(bars.left, bars.top, bars.right, 0);
            scroll.setPadding(scroll.getPaddingLeft(), scroll.getPaddingTop(),
                    scroll.getPaddingRight(), bars.bottom);
            ViewGroup.MarginLayoutParams params =
                    (ViewGroup.MarginLayoutParams) fab.getLayoutParams();
            int bottom = bars.bottom + dp(16);
            if (params.bottomMargin != bottom) {
                params.bottomMargin = bottom;
                fab.setLayoutParams(params);
            }
            return insets;
        });
        ViewCompat.requestApplyInsets(root);

        CollapsingToolbarLayout collapsing = findViewById(R.id.sidebar_collapsing);
        collapsing.setTitle(getString(R.string.sidebar_title));

        MaterialToolbar toolbar = findViewById(R.id.sidebar_toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        findViewById(R.id.sidebar_function_new).setOnClickListener(v ->
                notice(getString(R.string.sidebar_notice_new)));
        findViewById(R.id.sidebar_function_search).setOnClickListener(v ->
                notice(getString(R.string.notice_menu_demo, getString(R.string.sidebar_search))));
        findViewById(R.id.sidebar_function_folders).setOnClickListener(v ->
                notice(getString(R.string.notice_menu_demo, getString(R.string.sidebar_folders))));

        buildHistory(findViewById(R.id.sidebar_history));

        fab.setOnClickListener(v -> SettingsActivity.start(this));
    }

    private void buildHistory(LinearLayout host) {
        int muted = MaterialColors.getColor(host,
                com.google.android.material.R.attr.colorOnSurfaceVariant);

        TextView heading = new TextView(this);
        heading.setText(R.string.sidebar_history);
        heading.setTextSize(12);
        heading.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        heading.setTextColor(muted);
        heading.setPadding(dp(4), dp(2), dp(4), dp(6));
        host.addView(heading);

        for (int group = 0; group < SESSIONS.length; group++) {
            LinearLayout header = new LinearLayout(this);
            header.setOrientation(LinearLayout.HORIZONTAL);
            header.setGravity(Gravity.CENTER_VERTICAL);
            header.setPadding(dp(4), dp(8), dp(4), dp(8));
            header.setClickable(true);
            header.setFocusable(true);

            TextView label = new TextView(this);
            label.setText(GROUP_TITLES[group]);
            label.setTextSize(12);
            label.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            label.setTextColor(muted);
            header.addView(label, new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            ImageView chevron = new ImageView(this);
            chevron.setImageResource(R.drawable.ic_keyboard_arrow_down);
            chevron.setImageTintList(ColorStateList.valueOf(muted));
            header.addView(chevron, new LinearLayout.LayoutParams(dp(18), dp(18)));

            LinearLayout items = new LinearLayout(this);
            items.setOrientation(LinearLayout.VERTICAL);

            for (String title : SESSIONS[group]) {
                MaterialButton button = (MaterialButton) LayoutInflater.from(this)
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
        notice(getString(R.string.notice_menu_demo, title));
    }

    private void notice(String message) {
        Snackbar.make(root, message, Snackbar.LENGTH_SHORT).show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
