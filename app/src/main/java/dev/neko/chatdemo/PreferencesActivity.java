package dev.neko.chatdemo;

import android.app.Activity;
import android.content.Intent;
import android.widget.LinearLayout;

import com.google.android.material.card.MaterialCardView;

import dev.neko.chatdemo.ui.SettingsPageActivity;
import dev.neko.chatdemo.ui.SettingsUi;
import dev.neko.chatdemo.ui.UiTransitions;

/** One card group with the five preference pages. */
public class PreferencesActivity extends SettingsPageActivity {

    public static void start(Activity from) {
        UiTransitions.start(from, new Intent(from, PreferencesActivity.class), true);
    }

    @Override
    protected int titleRes() {
        return R.string.settings_preferences;
    }

    @Override
    protected void buildContent(LinearLayout content) {
        int primary = SettingsUi.color(content,
                com.google.android.material.R.attr.colorPrimaryContainer);
        int onPrimary = SettingsUi.color(content,
                com.google.android.material.R.attr.colorOnPrimaryContainer);

        LinearLayout group = SettingsUi.group(this, content, R.string.settings_preferences);
        addRow(group, R.drawable.ic_tune, R.string.settings_theme, R.string.settings_theme_sub,
                "theme", primary, onPrimary);
        addRow(group, R.drawable.ic_settings, R.string.settings_general, R.string.settings_general_sub,
                "general", primary, onPrimary);
        addRow(group, R.drawable.ic_animation, R.string.settings_ui, R.string.settings_ui_sub,
                "ui", primary, onPrimary);
        addRow(group, R.drawable.ic_chat_bubble, R.string.settings_notifications,
                R.string.settings_notifications_sub, "notifications", primary, onPrimary);
        addRow(group, R.drawable.ic_cloud, R.string.settings_network, R.string.settings_network_sub,
                "network", primary, onPrimary);
        SettingsUi.layoutGroup(group);
    }

    private void addRow(LinearLayout group, int icon, int title, int subtitle, String key,
                        int container, int onContainer) {
        MaterialCardView row = SettingsUi.row(this, group, icon,
                getString(title), getString(subtitle), container, onContainer);
        row.setOnClickListener(v -> SettingsDetailActivity.start(this, key));
    }
}
