package dev.neko.chatdemo;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.materialswitch.MaterialSwitch;

import dev.neko.chatdemo.ui.UiTransitions;

/** One settings sub page, opened with a shared axis X activity transition. */
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
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        UiTransitions.incoming(this, true);
        setContentView(R.layout.activity_settings_detail);

        boolean night = (getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(!night);
        controller.setAppearanceLightNavigationBars(!night);

        View root = findViewById(R.id.detail_root);
        View topBar = findViewById(R.id.detail_top_bar);
        View scroll = findViewById(R.id.detail_scroll);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            topBar.setPadding(bars.left + dp(8), bars.top, bars.right + dp(8), 0);
            scroll.setPadding(0, 0, 0, bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(root);

        findViewById(R.id.detail_back).setOnClickListener(v -> finish());

        String page = getIntent().getStringExtra(EXTRA_PAGE);
        bindPage(page == null ? "theme" : page);
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
        LayoutInflater inflater = getLayoutInflater();
        for (int[] row : rows) {
            View view = inflater.inflate(R.layout.item_settings_switch, content, false);
            ((ImageView) view.findViewById(R.id.row_icon)).setImageResource(row[1]);
            ((TextView) view.findViewById(R.id.row_title)).setText(row[0]);
            MaterialSwitch toggle = view.findViewById(R.id.row_switch);
            toggle.setChecked(row[2] == 1);
            view.setOnClickListener(v -> toggle.setChecked(!toggle.isChecked()));
            content.addView(view);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
