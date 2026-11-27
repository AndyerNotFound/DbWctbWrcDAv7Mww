package dev.neko.chatdemo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import dev.neko.chatdemo.ui.SettingsUi;
import dev.neko.chatdemo.ui.UiTransitions;

/** One settings sub page, built as a single card group of switches. */
public class SettingsDetailActivity extends AppCompatActivity {

    private static final String EXTRA_PAGE = "page";

    public static void start(Activity from, String page) {
        Intent intent = new Intent(from, SettingsDetailActivity.class);
        intent.putExtra(EXTRA_PAGE, page);
        UiTransitions.start(from, intent, true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SettingsUi.setupWindow(this);
        UiTransitions.incoming(this, true);
        setContentView(R.layout.activity_settings_detail);
        SettingsUi.applyInsets(findViewById(R.id.detail_root),
                findViewById(R.id.detail_top_bar), findViewById(R.id.detail_scroll));
        findViewById(R.id.detail_back).setOnClickListener(v -> finishAfterTransition());

        String page = getIntent().getStringExtra(EXTRA_PAGE);
        bindPage(page == null ? "theme" : page);
    }

    @Override
    public void onBackPressed() {
        finishAfterTransition();
    }

    private void bindPage(String page) {
        int titleRes;
        int[][] rows;
        switch (page) {
            case "general":
                titleRes = R.string.settings_general;
                rows = new int[][]{
                        {R.string.row_general_haptics, R.drawable.ic_tune, 1},
                        {R.string.row_general_drafts, R.drawable.ic_draft, 1},
                        {R.string.row_general_confirm, R.drawable.ic_delete, 0}
                };
                break;
            case "ui":
                titleRes = R.string.settings_ui;
                rows = new int[][]{
                        {R.string.row_ui_compact, R.drawable.ic_description, 0},
                        {R.string.row_ui_tokens, R.drawable.ic_data_usage, 1},
                        {R.string.row_ui_motion, R.drawable.ic_animation, 1}
                };
                break;
            case "notifications":
                titleRes = R.string.settings_notifications;
                rows = new int[][]{
                        {R.string.row_notif_notices, R.drawable.ic_chat_bubble, 1},
                        {R.string.row_notif_recording, R.drawable.ic_mic, 1},
                        {R.string.row_notif_sound, R.drawable.ic_bolt, 0}
                };
                break;
            case "network":
                titleRes = R.string.settings_network;
                rows = new int[][]{
                        {R.string.row_net_proxy, R.drawable.ic_cloud, 0},
                        {R.string.row_net_metered, R.drawable.ic_data_usage, 1},
                        {R.string.row_net_retry, R.drawable.ic_sync, 1}
                };
                break;
            case "theme":
            default:
                titleRes = R.string.settings_theme;
                rows = new int[][]{
                        {R.string.row_theme_dynamic, R.drawable.ic_auto_awesome, 1},
                        {R.string.row_theme_amoled, R.drawable.ic_power_settings_new, 0},
                        {R.string.row_theme_contrast, R.drawable.ic_diamond, 0}
                };
                break;
        }

        ((TextView) findViewById(R.id.detail_title)).setText(titleRes);

        LinearLayout content = findViewById(R.id.detail_content);
        int container = SettingsUi.color(content,
                com.google.android.material.R.attr.colorPrimaryContainer);
        int onContainer = SettingsUi.color(content,
                com.google.android.material.R.attr.colorOnPrimaryContainer);

        LinearLayout group = SettingsUi.group(this, content, (CharSequence) null);
        for (int[] row : rows) {
            SettingsUi.switchRow(this, group, row[1], getString(row[0]), row[2] == 1,
                    container, onContainer);
        }
        SettingsUi.finishGroup(group);
    }
}
