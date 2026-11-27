package dev.neko.chatdemo;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

import dev.neko.chatdemo.ui.SettingsAdapter;
import dev.neko.chatdemo.ui.UiTransitions;

/** Multi-section settings home: colour mode, preferences and the provider block. */
public class SettingsActivity extends AppCompatActivity implements SettingsAdapter.Callbacks {

    public static final String PREFS = "neko_prefs";
    public static final String KEY_COLOR_MODE = "color_mode";

    private SettingsAdapter adapter;
    private SharedPreferences prefs;
    private List<SettingsAdapter.ProviderItem> providers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        UiTransitions.incoming(this, true);
        setContentView(R.layout.activity_settings);

        boolean night = (getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(!night);
        controller.setAppearanceLightNavigationBars(!night);

        View root = findViewById(R.id.settings_root);
        View topBar = findViewById(R.id.settings_top_bar);
        RecyclerView list = findViewById(R.id.settings_list);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            topBar.setPadding(bars.left + dp(8), bars.top, bars.right + dp(8), 0);
            list.setPadding(0, 0, 0, bars.bottom + dp(28));
            return insets;
        });
        ViewCompat.requestApplyInsets(root);

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        providers = buildProviders();

        adapter = new SettingsAdapter(this, providers, readColorMode(), this);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        ItemTouchHelper drag = new ItemTouchHelper(new DragCallback());
        drag.attachToRecyclerView(list);

        findViewById(R.id.settings_back).setOnClickListener(v -> finish());
    }

    private int readColorMode() {
        int mode = prefs.getInt(KEY_COLOR_MODE, 0);
        return mode < 0 || mode > 2 ? 0 : mode;
    }

    private List<SettingsAdapter.ProviderItem> buildProviders() {
        List<SettingsAdapter.ProviderItem> items = new ArrayList<>();
        items.add(new SettingsAdapter.ProviderItem(
                "OpenAI", "O", "https://api.openai.com", "sk-live-1234", 12));
        items.add(new SettingsAdapter.ProviderItem(
                "Anthropic", "A", "https://api.anthropic.com", "sk-ant-5678", 8));
        items.add(new SettingsAdapter.ProviderItem(
                "Google", "G", "https://generativelanguage.googleapis.com", "AIza-9012", 5));
        items.add(new SettingsAdapter.ProviderItem(
                "OpenRouter", "R", "https://openrouter.ai/api", "sk-or-3456", 42));
        items.add(new SettingsAdapter.ProviderItem(
                "Local", "L", "http://127.0.0.1:8080", "none", 3));
        return items;
    }

    // ---------------------------------------------------------------------------------------
    // Callbacks

    @Override
    public void onColorMode(int mode) {
        prefs.edit().putInt(KEY_COLOR_MODE, mode).apply();
        AppCompatDelegate.setDefaultNightMode(mode == 0
                ? AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                : mode == 1 ? AppCompatDelegate.MODE_NIGHT_NO : AppCompatDelegate.MODE_NIGHT_YES);
        adapter.setColorMode(mode);
        notice(getString(R.string.settings_notice_mode, getString(mode == 1
                ? R.string.settings_color_light
                : mode == 2 ? R.string.settings_color_dark : R.string.settings_color_follow)));
    }

    @Override
    public void onPreference(@NonNull String key) {
        SettingsDetailActivity.start(this, key);
    }

    @Override
    public void onImportProvider() {
        notice(getString(R.string.settings_notice_import));
    }

    @Override
    public void onAddProvider() {
        notice(getString(R.string.settings_notice_add));
    }

    @Override
    public void onFetchModels(@NonNull SettingsAdapter.ProviderItem provider) {
        notice(getString(R.string.settings_notice_models, provider.baseUrl));
    }

    private void notice(String message) {
        View root = findViewById(R.id.settings_root);
        Snackbar.make(root, message, Snackbar.LENGTH_SHORT).show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private class DragCallback extends ItemTouchHelper.SimpleCallback {

        DragCallback() {
            super(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
        }

        @Override
        public int getMovementFlags(@NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder) {
            int drag = adapter.isProviderPosition(viewHolder.getBindingAdapterPosition())
                    ? ItemTouchHelper.UP | ItemTouchHelper.DOWN : 0;
            return makeMovementFlags(drag, 0);
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView,
                              @NonNull RecyclerView.ViewHolder viewHolder,
                              @NonNull RecyclerView.ViewHolder target) {
            return adapter.moveProvider(viewHolder.getBindingAdapterPosition(),
                    target.getBindingAdapterPosition());
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        }

        @Override
        public boolean isLongPressDragEnabled() {
            return true;
        }
    }
}
