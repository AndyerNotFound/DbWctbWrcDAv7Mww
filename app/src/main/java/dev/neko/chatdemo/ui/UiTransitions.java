package dev.neko.chatdemo.ui;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.transition.Fade;
import android.transition.Slide;
import android.transition.Transition;
import android.transition.TransitionSet;
import android.view.Gravity;
import android.view.Window;
import android.view.animation.DecelerateInterpolator;

/**
 * Shared axis X activity transitions.
 *
 * <p>{@code forward = true} reads like "deeper": both pages travel left, the new one enters from
 * the right. {@code forward = false} is the side panel direction: both pages travel right, the new
 * one enters from the left.
 *
 * <p>Activity windows only accept framework transitions, so the shared axis is built from a
 * {@link Slide} and a {@link Fade} running together rather than {@code MaterialSharedAxis}.
 */
public final class UiTransitions {

    private static final int DURATION = 320;

    public static void outgoing(Activity activity, boolean forward) {
        int edge = forward ? Gravity.START : Gravity.END;
        Window window = activity.getWindow();
        window.setExitTransition(axis(edge));
        window.setReenterTransition(axis(edge));
    }

    public static void incoming(Activity activity, boolean forward) {
        int edge = forward ? Gravity.END : Gravity.START;
        Window window = activity.getWindow();
        window.setEnterTransition(axis(edge));
        window.setReturnTransition(axis(edge));
    }

    /**
     * Opens a full page panel: the panel slides in from the left and the page underneath stays
     * put, so closing it slides the same panel back out to the left.
     */
    public static void startPanel(Activity from, Intent intent) {
        from.getWindow().setExitTransition(null);
        from.getWindow().setReenterTransition(null);
        from.startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(from).toBundle());
    }

    /** Companion of {@link #startPanel(Activity, Intent)} for the panel activity itself. */
    public static void incomingPanel(Activity activity) {
        Window window = activity.getWindow();
        window.setEnterTransition(axis(Gravity.START));
        window.setReturnTransition(axis(Gravity.START));
    }

    public static void start(Activity from, Intent intent, boolean forward) {
        outgoing(from, forward);
        from.startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(from).toBundle());
    }

    private static Transition axis(int slideEdge) {
        TransitionSet set = new TransitionSet()
                .setOrdering(TransitionSet.ORDERING_TOGETHER)
                .addTransition(new Slide(slideEdge))
                .addTransition(new Fade());
        set.setDuration(DURATION);
        set.setInterpolator(new DecelerateInterpolator());
        return set;
    }

    private UiTransitions() {
    }
}
