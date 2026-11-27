package dev.neko.chatdemo;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.color.DynamicColors;

/**
 * Applies the wallpaper-based Material You palette on Android 12+. The teal/clay palette in
 * values/colors.xml stays as the fallback for older releases and for devices without dynamic
 * colour support.
 */
public class NekoApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        int mode = getSharedPreferences(SettingsActivity.PREFS, MODE_PRIVATE)
                .getInt(SettingsActivity.KEY_COLOR_MODE, 0);
        AppCompatDelegate.setDefaultNightMode(mode == 1
                ? AppCompatDelegate.MODE_NIGHT_NO
                : mode == 2 ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        DynamicColors.applyToActivitiesIfAvailable(this);
    }
}
