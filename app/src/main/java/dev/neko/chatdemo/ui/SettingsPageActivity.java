package dev.neko.chatdemo.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.appbar.MaterialToolbar;

import dev.neko.chatdemo.R;
import dev.neko.chatdemo.SettingsActivity;

/**
 * Shared chrome for the settings pages: collapsing title, insets, edge to edge and the page
 * transition. A page opened from a settings row morphs out of that row; every other page uses the
 * shared axis X pair.
 */
public abstract class SettingsPageActivity extends AppCompatActivity {

    protected SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Must run before super.onCreate so the shared element callback is installed before the
        // content view is attached.
        boolean morphing = UiTransitions.prepareMorph(this);
        super.onCreate(savedInstanceState);
        SettingsUi.setupWindow(this);
        prefs = getSharedPreferences(SettingsActivity.PREFS, MODE_PRIVATE);

        setContentView(R.layout.activity_settings);
        if (morphing) {
            UiTransitions.enterMorph(this);
        } else {
            UiTransitions.incoming(this, true);
        }

        View root = findViewById(R.id.settings_root);
        View appBar = findViewById(R.id.settings_app_bar);
        View scroll = findViewById(R.id.settings_scroll);
        SettingsUi.applyInsets(root, appBar, scroll);

        CollapsingToolbarLayout collapsing = findViewById(R.id.settings_collapsing);
        SettingsUi.setTitle(collapsing, getString(titleRes()));

        MaterialToolbar toolbar = findViewById(R.id.settings_toolbar);
        toolbar.setNavigationOnClickListener(v -> UiTransitions.close(this));

        buildContent(findViewById(R.id.settings_content));
    }

    protected abstract int titleRes();

    protected abstract void buildContent(LinearLayout content);

    @Override
    public void onBackPressed() {
        UiTransitions.close(this);
    }
}
