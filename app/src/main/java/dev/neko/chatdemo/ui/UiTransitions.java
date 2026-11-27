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
 * the right. {@code forward = false} is the back direction: both pages travel right, the new one
 * enters from the left.
 *
 * <p>Activity windows only accept framework transitions, so the shared axis is built from a
 * {@link Slide} and a {@link Fade} running together rather than {@code MaterialSharedAxis}.
 */
public final class UiTransitions {

    private static final int DURATION = 320;

    /** Exit / reenter pair for the page that stays underneath. */
    public static void outgoing(Activity activity, boolean forward) {
        int edge = forward ? Gravity.START : Gravity.END;
        Window window = activity.getWindow();
        window.setExitTransition(axis(edge));
        window.setReenterTransition(axis(edge));
    }

    /** Enter / return pair for the page that comes on top. */
    public static void incoming(Activity activity, boolean forward) {
        int edge = forward ? Gravity.END : Gravity.START;
        Window window = activity.getWindow();
        window.setEnterTransition(axis(edge));
        window.setReturnTransition(axis(edge));
    }

    /**
     * Pushes {@code intent} with the shared axis X pair. Pair it with
     * {@link #incoming(Activity, boolean)} and {@code finishAfterTransition()} on the new page.
     */
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
