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
 * Host of the sidebar panel. The panel itself is a fragment, so a day group can grow out of its
 * card with a container transform; the FAB stays on the host and opens settings.
 */
public class SidebarActivity extends AppCompatActivity implements SidebarFragment.Callbacks {

    private static final String TAG_PANEL = "SidebarFragment::panel";

    private View root;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SettingsUi.setupWindow(this);
        // Matches the launch in MainActivity: the panel is the page to the left of the chat.
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

        fab.setOnClickListener(v -> SettingsActivity.start(this));

        // Back closes the group page first and only then leaves the panel, and leaving always
        // goes through the shared axis X return instead of a plain finish.
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                FragmentManager fragments = getSupportFragmentManager();
                if (fragments.getBackStackEntryCount() > 0) {
                    fragments.popBackStack();
                    return;
                }
                setEnabled(false);
                finishAfterTransition();
            }
        });
    }

    @Override
    public void onCollapseSidebar() {
        finishAfterTransition();
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
