package dev.neko.chatdemo;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.snackbar.Snackbar;

import dev.neko.chatdemo.ui.SettingsUi;
import dev.neko.chatdemo.ui.SidebarFragment;
import dev.neko.chatdemo.ui.UiTransitions;

/**
 * Host of the sidebar panel. The panel is one fragment and a day group is a child of it, so
 * expanding and collapsing a group never leaves this page. The FAB grows into Settings with a
 * container transform.
 */
public class SidebarActivity extends AppCompatActivity implements SidebarFragment.Callbacks {

    private static final String TAG_PANEL = "SidebarFragment::panel";

    private View root;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SettingsUi.setupWindow(this);
        // The panel is the page to the left of the chat, so it enters and leaves on that side.
        UiTransitions.incoming(this, false);
        setContentView(R.layout.activity_sidebar);
        root = findViewById(R.id.sidebar_root);

        View fab = findViewById(R.id.sidebar_fab);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
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

        FragmentManager manager = getSupportFragmentManager();
        if (manager.findFragmentByTag(TAG_PANEL) == null) {
            manager.beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.sidebar_container, new SidebarFragment(), TAG_PANEL)
                    .commit();
        }

        fab.setOnClickListener(v -> SettingsActivity.startFrom(this, v));

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                SidebarFragment panel =
                        (SidebarFragment) getSupportFragmentManager().findFragmentByTag(TAG_PANEL);
                if (panel != null && panel.isGroupOpen()) {
                    // Collapse the group first: same page, same transform in reverse.
                    panel.collapseGroup();
                    return;
                }
                setEnabled(false);
                UiTransitions.close(SidebarActivity.this);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // The FAB is the landing spot of the settings morph, so make sure it is back after a
        // return where the shared element callback left it transparent.
        View fab = findViewById(R.id.sidebar_fab);
        if (fab != null) {
            fab.setAlpha(1f);
            fab.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onCollapseSidebar() {
        UiTransitions.close(this);
    }

    @Override
    public void onNewSession() {
        notice(getString(R.string.sidebar_notice_new));
    }

    @Override
    public void onSearch() {
        notice(getString(R.string.notice_menu_demo, getString(R.string.sidebar_search)));
    }

    @Override
    public void onFolders() {
        notice(getString(R.string.notice_menu_demo, getString(R.string.sidebar_folders)));
    }

    private void notice(String message) {
        Snackbar.make(root, message, Snackbar.LENGTH_SHORT).show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
