package dev.neko.chatdemo.ui;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.view.Window;

import com.google.android.material.transition.MaterialSharedAxis;

/**
 * Shared axis X activity transitions.
 *
 * <p>{@code forward = true} reads like "deeper": the old page slides left and the new one comes
 * from the right. {@code forward = false} is the side-panel direction: the old page slides right
 * and the new one comes from the left.
 */
public final class UiTransitions {

    private static final int DURATION = 320;

    public static void outgoing(Activity activity, boolean forward) {
        Window window = activity.getWindow();
        window.setExitTransition(axis(forward));
        window.setReenterTransition(axis(!forward));
    }

    public static void incoming(Activity activity, boolean forward) {
        Window window = activity.getWindow();
        window.setEnterTransition(axis(forward));
        window.setReturnTransition(axis(!forward));
    }

    public static void start(Activity from, Intent intent, boolean forward) {
        outgoing(from, forward);
        from.startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(from).toBundle());
    }

    private static MaterialSharedAxis axis(boolean forward) {
        MaterialSharedAxis transition = new MaterialSharedAxis(MaterialSharedAxis.X, forward);
        transition.setDuration(DURATION);
        return transition;
    }

    private UiTransitions() {
    }
}
