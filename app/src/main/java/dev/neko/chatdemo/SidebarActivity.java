package dev.neko.chatdemo;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.snackbar.Snackbar;

import dev.neko.chatdemo.ui.UiTransitions;

/** Full page session browser. Opens over the chat with a shared axis X transition. */
public class SidebarActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        UiTransitions.incoming(this, false);
        setContentView(R.layout.activity_sidebar);

        boolean night = (getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(!night);
        controller.setAppearanceLightNavigationBars(!night);

        View root = findViewById(R.id.sidebar_root);
        View topBar = findViewById(R.id.sidebar_top_bar);
        View scroll = findViewById(R.id.sidebar_scroll);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            topBar.setPadding(bars.left + dp(8), bars.top, bars.right + dp(8), 0);
            scroll.setPadding(0, 0, 0, bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(root);

        findViewById(R.id.sidebar_back).setOnClickListener(v -> finish());
        findViewById(R.id.sidebar_new_session).setOnClickListener(v ->
                Snackbar.make(root, R.string.sidebar_notice_new, Snackbar.LENGTH_SHORT).show());
        findViewById(R.id.sidebar_settings).setOnClickListener(v ->
                UiTransitions.start(this, new Intent(this, SettingsActivity.class), true));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
