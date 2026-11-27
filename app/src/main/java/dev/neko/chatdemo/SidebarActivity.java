package dev.neko.chatdemo;

import android.app.ActivityOptions;
import android.content.Intent;
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
import androidx.transition.TransitionSet;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.transition.MaterialFade;
import com.google.android.material.transition.platform.MaterialContainerTransformSharedElementCallback;

import dev.neko.chatdemo.ui.SettingsUi;
import dev.neko.chatdemo.ui.UiTransitions;

/**
 * Full page session browser: a collapse control and the panel title on top, the quick entries
 * pinned underneath as one full width column, then the chat history in its own scrolling area.
 * The bottom left FAB grows into the settings page with a container transform.
 */
public class SidebarActivity extends AppCompatActivity {

    private static final String SHARED_CONTAINER = "settings_container";

    private static final String[][] SESSIONS = {
            {"Late night refactor", "Sheet motion study", "Palette pass"},
            {"Dock clipping", "Wavy progress", "Map caption"},
            {"Apr 12 · build log", "Mar 30 · insets", "Mar 21 · tabs"}
    };
    private static final int[] GROUP_TITLES = {
            R.string.group_today, R.string.group_yesterday, R.string.group_earlier
    };

    private MaterialButton current;
    private View root;
    private LinearLayout historyHost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // The FAB hands its bounds to the settings page, so this window keeps a shared element
        // callback for both directions of the morph.
        setExitSharedElementCallback(new MaterialContainerTransformSharedElementCallback());
        super.onCreate(savedInstanceState);
        SettingsUi.setupWindow(this);
        UiTransitions.incomingPanel(this);
        setContentView(R.layout.activity_sidebar);
        root = findViewById(R.id.sidebar_root);

        View header = findViewById(R.id.sidebar_header);
        View scroll = findViewById(R.id.sidebar_scroll);
        View fab = findViewById(R.id.sidebar_fab);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            header.setPadding(header.getPaddingLeft(), bars.top,
                    header.getPaddingRight(), header.getPaddingBottom());
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

        MaterialToolbar toolbar = findViewById(R.id.sidebar_toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        findViewById(R.id.sidebar_function_new).setOnClickListener(v ->
                notice(getString(R.string.sidebar_notice_new)));
        findViewById(R.id.sidebar_function_search).setOnClickListener(v ->
                notice(getString(R.string.notice_menu_demo, getString(R.string.sidebar_search))));
        findViewById(R.id.sidebar_function_folders).setOnClickListener(v ->
                notice(getString(R.string.notice_menu_demo, getString(R.string.sidebar_folders))));

        historyHost = findViewById(R.id.sidebar_history);
        buildHistory(historyHost);

        fab.setOnClickListener(this::openSettings);
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

            header.setOnClickListener(v -> toggleGroup(items, chevron));
        }
    }

    /**
     * Expands or collapses one day group. The rows fade out and in on the M3 fade curve instead of
     * snapping, while {@link ChangeBounds} closes the gap so the groups below slide into place.
     */
    private void toggleGroup(LinearLayout items, ImageView chevron) {
        boolean show = items.getVisibility() != View.VISIBLE;

        MaterialFade fade = new MaterialFade();

        TransitionSet set = new TransitionSet()
                .setOrdering(TransitionSet.ORDERING_TOGETHER)
                .addTransition(fade)
                .addTransition(new ChangeBounds());
        TransitionManager.beginDelayedTransition(historyHost, set);

        items.setVisibility(show ? View.VISIBLE : View.GONE);
        chevron.animate().rotation(show ? 0f : -90f).setDuration(200).start();
    }

    private void choose(MaterialButton button, String title) {
        if (current != null && current != button) {
            current.setChecked(false);
        }
        button.setChecked(true);
        current = button;
        notice(getString(R.string.notice_menu_demo, title));
    }

    /** Grows the settings page out of the FAB and shrinks it back on return. */
    private void openSettings(View source) {
        Intent intent = new Intent(this, SettingsActivity.class)
                .putExtra(SettingsActivity.EXTRA_CONTAINER, true);
        getWindow().setExitTransition(null);
        getWindow().setReenterTransition(null);
        ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(
                this, source, SHARED_CONTAINER);
        startActivity(intent, options.toBundle());
    }

    private void notice(String message) {
        Snackbar.make(root, message, Snackbar.LENGTH_SHORT).show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
