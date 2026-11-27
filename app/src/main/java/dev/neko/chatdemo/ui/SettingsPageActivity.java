package dev.neko.chatdemo.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import dev.neko.chatdemo.R;
import dev.neko.chatdemo.SettingsActivity;

/** Shared chrome for the settings pages: top bar, insets, edge to edge and the page transition. */
public abstract class SettingsPageActivity extends AppCompatActivity {

    protected SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SettingsUi.setupWindow(this);
        prefs = getSharedPreferences(SettingsActivity.PREFS, MODE_PRIVATE);

        UiTransitions.incoming(this, true);

        setContentView(R.layout.activity_settings);
        SettingsUi.applyInsets(findViewById(R.id.settings_root),
                findViewById(R.id.settings_top_bar), findViewById(R.id.settings_scroll));

        ((TextView) findViewById(R.id.settings_page_title)).setText(titleRes());
        findViewById(R.id.settings_back).setOnClickListener(v -> finish());
        buildContent(findViewById(R.id.settings_content));
    }

    protected abstract int titleRes();

    protected abstract void buildContent(LinearLayout content);
}
