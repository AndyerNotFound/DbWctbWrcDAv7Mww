package dev.neko.chatdemo;

import android.app.Application;

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
        DynamicColors.applyToActivitiesIfAvailable(this);
    }
}
