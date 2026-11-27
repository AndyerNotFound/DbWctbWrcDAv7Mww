package dev.neko.chatdemo;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.snackbar.Snackbar;

import dev.neko.chatdemo.ui.SettingsPageActivity;
import dev.neko.chatdemo.ui.SettingsUi;
import dev.neko.chatdemo.ui.UiTransitions;

/** Settings home: preference cards for general options, models and services, data and about. */
public class SettingsActivity extends SettingsPageActivity {

    public static final String PREFS = "neko_prefs";
    public static final String KEY_COLOR_MODE = "color_mode";

    public static void start(Activity from) {
        UiTransitions.start(from, new Intent(from, SettingsActivity.class), true);
    }

    @Override
    protected int titleRes() {
        return R.string.settings_title;
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
        int secondary = SettingsUi.color(content,
                com.google.android.material.R.attr.colorSecondaryContainer);
        int onSecondary = SettingsUi.color(content,
                com.google.android.material.R.attr.colorOnSecondaryContainer);

        LinearLayout general = SettingsUi.group(this, content, R.string.settings_general);
        View mode = getLayoutInflater().inflate(R.layout.item_pref_form_mode, general, false);
        bindColorMode(mode);
        general.addView(mode);

        View preferences = SettingsUi.row(this, general, R.drawable.ic_tune,
                getString(R.string.settings_preferences), getString(R.string.settings_preferences_sub),
                primary, onPrimary);
        preferences.setOnClickListener(v -> PreferencesActivity.start(this));

        View language = SettingsUi.row(this, general, R.drawable.ic_language,
                getString(R.string.settings_language), null, primary, onPrimary);
        SettingsUi.setValue(language, getString(R.string.settings_follow_system_value));
        language.setOnClickListener(v -> demo(R.string.settings_language));
        SettingsUi.finishGroup(general);

        LinearLayout models = SettingsUi.group(this, content, R.string.settings_models_group);
        View defaultModel = SettingsUi.row(this, models, R.drawable.ic_smart_toy,
                getString(R.string.settings_default_model), null, tertiary, onTertiary);
        SettingsUi.setValue(defaultModel, "GPT-5.4");
        defaultModel.setOnClickListener(v -> demo(R.string.settings_default_model));

        View providers = SettingsUi.row(this, models, R.drawable.ic_cloud,
                getString(R.string.settings_providers), getString(R.string.settings_providers_sub),
                tertiary, onTertiary);
        providers.setOnClickListener(v -> ProvidersActivity.start(this));

        View search = SettingsUi.row(this, models, R.drawable.ic_search,
                getString(R.string.settings_search_service), null, tertiary, onTertiary);
        SettingsUi.setValue(search, "Tavily");
        search.setOnClickListener(v -> demo(R.string.settings_search_service));

        View speech = SettingsUi.row(this, models, R.drawable.ic_mic,
                getString(R.string.settings_speech), getString(R.string.settings_speech_sub),
                tertiary, onTertiary);
        speech.setOnClickListener(v -> demo(R.string.settings_speech));
        SettingsUi.finishGroup(models);

        LinearLayout data = SettingsUi.group(this, content, R.string.settings_data_group);
        View backup = SettingsUi.row(this, data, R.drawable.ic_upload_file,
                getString(R.string.settings_backup), getString(R.string.settings_backup_sub),
                secondary, onSecondary);
        backup.setOnClickListener(v -> demo(R.string.settings_backup));

        View storage = SettingsUi.row(this, data, R.drawable.ic_storage,
                getString(R.string.settings_storage), null, secondary, onSecondary);
        SettingsUi.setValue(storage, getString(R.string.settings_storage_value));
        storage.setOnClickListener(v -> demo(R.string.settings_storage));
        SettingsUi.finishGroup(data);

        LinearLayout about = SettingsUi.group(this, content, R.string.settings_about_group);
        View aboutApp = SettingsUi.row(this, about, R.drawable.ic_info,
                getString(R.string.settings_about_app), null, primary, onPrimary);
        SettingsUi.setValue(aboutApp, "1.0.0");
        aboutApp.setOnClickListener(v -> demo(R.string.settings_about_app));

        View docs = SettingsUi.row(this, about, R.drawable.ic_description,
                getString(R.string.settings_docs), getString(R.string.settings_docs_sub),
                primary, onPrimary);
        docs.setOnClickListener(v -> demo(R.string.settings_docs));
        SettingsUi.finishGroup(about);
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

    private void demo(@StringRes int titleRes) {
        Snackbar.make(findViewById(R.id.settings_root),
                getString(R.string.notice_menu_demo, getString(titleRes)),
                Snackbar.LENGTH_SHORT).show();
    }
}
