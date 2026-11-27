package dev.neko.chatdemo;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityOptionsCompat;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;

import dev.neko.chatdemo.ui.SettingsPageActivity;
import dev.neko.chatdemo.ui.SettingsUi;

/** Settings home: colour mode, preferences and providers as card groups. */
public class SettingsActivity extends SettingsPageActivity {

    public static final String PREFS = "neko_prefs";
    public static final String KEY_COLOR_MODE = "color_mode";
    private static final String EXTRA_CONTAINER = "container";
    private static final String SHARED_CONTAINER = "settings_container";

    public static void start(Activity from) {
        dev.neko.chatdemo.ui.UiTransitions.start(from,
                new Intent(from, SettingsActivity.class), true);
    }

    /** Opens the page with a container transform growing out of {@code source}. */
    public static void startFrom(Activity from, View source) {
        Intent intent = new Intent(from, SettingsActivity.class)
                .putExtra(EXTRA_CONTAINER, true);
        from.getWindow().setExitTransition(null);
        from.getWindow().setReenterTransition(null);
        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                from, source, SHARED_CONTAINER);
        from.startActivity(intent, options.toBundle());
    }

    @Override
    protected int titleRes() {
        return R.string.settings_title;
    }

    @Override
    protected boolean usesContainerTransform() {
        return getIntent().getBooleanExtra(EXTRA_CONTAINER, false);
    }

    @Override
    protected void buildContent(LinearLayout content) {
        int primary = SettingsUi.color(content,
                com.google.android.material.R.attr.colorPrimaryContainer);
        int onPrimary = SettingsUi.color(content,
                com.google.android.material.R.attr.colorOnPrimaryContainer);
        int tertiary = SettingsUi.color(content,
                com.google.android.material.R.attr.colorTertiaryContainer);
        int onTertiary = SettingsUi.color(content,
                com.google.android.material.R.attr.colorOnTertiaryContainer);

        LinearLayout general = SettingsUi.group(this, content, R.string.settings_general);
        View form = getLayoutInflater().inflate(R.layout.item_pref_form_mode, general, false);
        bindColorMode(form);
        general.addView(form);

        MaterialCardView preferences = SettingsUi.row(this, general, R.drawable.ic_tune,
                getString(R.string.settings_preferences), getString(R.string.settings_preferences_sub),
                primary, onPrimary);
        preferences.setOnClickListener(v -> PreferencesActivity.start(this));
        SettingsUi.layoutGroup(general);

        LinearLayout providers = SettingsUi.group(this, content, R.string.settings_providers);
        MaterialCardView providerRow = SettingsUi.row(this, providers, R.drawable.ic_smart_toy,
                getString(R.string.settings_providers), getString(R.string.settings_providers_sub),
                tertiary, onTertiary);
        providerRow.setOnClickListener(v -> ProvidersActivity.start(this));
        SettingsUi.layoutGroup(providers);
    }

    private void bindColorMode(View form) {
        MaterialButtonToggleGroup group = form.findViewById(R.id.color_mode_group);
        int mode = prefs.getInt(KEY_COLOR_MODE, 0);
        group.check(mode == 1 ? R.id.color_mode_light
                : mode == 2 ? R.id.color_mode_dark : R.id.color_mode_follow);
        group.addOnButtonCheckedListener((g, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            int picked = checkedId == R.id.color_mode_light ? 1
                    : checkedId == R.id.color_mode_dark ? 2 : 0;
            prefs.edit().putInt(KEY_COLOR_MODE, picked).apply();
            AppCompatDelegate.setDefaultNightMode(picked == 0
                    ? AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    : picked == 1 ? AppCompatDelegate.MODE_NIGHT_NO
                    : AppCompatDelegate.MODE_NIGHT_YES);
        });
    }
}
