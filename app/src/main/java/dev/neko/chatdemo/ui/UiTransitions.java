package dev.neko.chatdemo.ui;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.transition.Fade;
import android.transition.Slide;
import android.transition.Transition;
import android.transition.TransitionSet;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.animation.DecelerateInterpolator;

import androidx.core.view.ViewCompat;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.transition.platform.MaterialContainerTransform;
import com.google.android.material.transition.platform.MaterialContainerTransformSharedElementCallback;

import dev.neko.chatdemo.R;

/**
 * Activity level transitions.
 *
 * <p>Two kinds are used:
 *
 * <ul>
 *   <li><b>Shared axis X</b> — a plain page push: both pages travel sideways, the new one enters
 *       from the right, or from the left when {@code forward = false} (a page that lives to the
 *       left of the current one, like the sidebar).
 *   <li><b>Container transform</b> — a source view grows into the whole page and shrinks back into
 *       it on return. Used when a page is opened from a specific control: the sidebar FAB, a
 *       settings row.
 * </ul>
 *
 * <p>Both kinds remember whether the entrance started and finished. A back press that arrives mid
 * entrance is queued and played once the entrance is done, so the page always retraces its own
 * path instead of cutting.
 */
public final class UiTransitions {

    private static final int DURATION = 320;
    private static final String EXTRA_MORPH = "neko:morph";

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
        window.setEnterTransition(track(activity, axis(edge)));
        window.setReturnTransition(axis(edge));
    }

    /** Pushes {@code intent} with the shared axis X pair. */
    public static void start(Activity from, Intent intent, boolean forward) {
        outgoing(from, forward);
        from.startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(from).toBundle());
    }

    // ---------------------------------------------------------------- container transform

    /**
     * Call before {@code super.onCreate}. Returns true when this page was opened from a source
     * view and should morph instead of sliding.
     */
    public static boolean prepareMorph(Activity activity) {
        if (morphName(activity) == null) {
            return false;
        }
        // Both have to be in place before the content view is installed, otherwise the framework
        // has nothing to map the shared element onto.
        activity.getWindow().requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS);
        activity.setEnterSharedElementCallback(new MaterialContainerTransformSharedElementCallback());
        return true;
    }

    /** Call right after {@code setContentView} on a page that {@link #prepareMorph} accepted. */
    public static void enterMorph(Activity activity) {
        String name = morphName(activity);
        if (name == null) {
            return;
        }
        View content = activity.findViewById(android.R.id.content);
        ViewCompat.setTransitionName(content, name);

        Window window = activity.getWindow();
        window.setEnterTransition(null);
        window.setSharedElementEnterTransition(track(activity, morph(activity, true)));
        window.setSharedElementReturnTransition(morph(activity, false));
    }

    /** Opens {@code intent} by growing it out of {@code source}, and back into it on return. */
    public static void startMorph(Activity from, View source, Intent intent) {
        // A name already set on the view (layout attribute or a stable per-row name) survives
        // activity recreation, so the return transform still finds its source.
        String name = ViewCompat.getTransitionName(source);
        if (name == null) {
            name = "neko_morph_" + Integer.toHexString(System.identityHashCode(source));
            ViewCompat.setTransitionName(source, name);
        }
        intent.putExtra(EXTRA_MORPH, name);

        // The container carries the whole animation, so the source page neither slides out nor
        // slides back in.
        from.setExitSharedElementCallback(new MaterialContainerTransformSharedElementCallback());
        from.getWindow().setExitTransition(null);
        from.getWindow().setReenterTransition(null);
        ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(from, source, name);
        from.startActivity(intent, options.toBundle());
    }

    private static String morphName(Activity activity) {
        Intent intent = activity.getIntent();
        return intent == null ? null : intent.getStringExtra(EXTRA_MORPH);
    }

    private static MaterialContainerTransform morph(Activity activity, boolean entering) {
        MaterialContainerTransform transform = new MaterialContainerTransform(activity, entering);
        transform.setAllContainerColors(MaterialColors.getColor(
                activity.findViewById(android.R.id.content),
                com.google.android.material.R.attr.colorSurface));
        transform.addTarget(android.R.id.content);
        return transform;
    }

    // ---------------------------------------------------------------- interruption

    /**
     * Leaves the page along the path it arrived on. When the entrance is still running the exit is
     * queued until it finishes, which keeps the return transition instead of a hard cut. When the
     * entrance never started there is nothing to retrace and the page just closes.
     */
    public static void close(Activity activity) {
        Window window = activity.getWindow();
        View decor = window.getDecorView();

        Transition enter = window.getSharedElementEnterTransition();
        if (enter == null) {
            enter = window.getEnterTransition();
        }

        boolean started = Boolean.TRUE.equals(decor.getTag(R.id.neko_enter_started));
        boolean done = Boolean.TRUE.equals(decor.getTag(R.id.neko_enter_done));

        if (enter == null || done || !started) {
            activity.finishAfterTransition();
            return;
        }
        if (Boolean.TRUE.equals(decor.getTag(R.id.neko_exit_queued))) {
            return;
        }
        decor.setTag(R.id.neko_exit_queued, true);
        enter.addListener(new Listener() {
            @Override
            public void onTransitionEnd(Transition transition) {
                activity.finishAfterTransition();
            }

            @Override
            public void onTransitionCancel(Transition transition) {
                activity.finishAfterTransition();
            }
        });
        // Safety net: if the entrance never reports back, the page still closes instead of
        // swallowing the back press.
        decor.postDelayed(() -> {
            if (!activity.isFinishing()) {
                activity.finishAfterTransition();
            }
        }, enter.getDuration() < 0 ? 600 : enter.getDuration() + 120);
    }

    /** Remembers how far the entrance got, so {@link #close(Activity)} knows whether to wait. */
    private static Transition track(Activity activity, Transition transition) {
        View decor = activity.getWindow().getDecorView();
        decor.setTag(R.id.neko_enter_started, false);
        decor.setTag(R.id.neko_enter_done, false);
        transition.addListener(new Listener() {
            @Override
            public void onTransitionStart(Transition t) {
                decor.setTag(R.id.neko_enter_started, true);
            }

            @Override
            public void onTransitionEnd(Transition t) {
                decor.setTag(R.id.neko_enter_done, true);
            }

            @Override
            public void onTransitionCancel(Transition t) {
                decor.setTag(R.id.neko_enter_done, true);
            }
        });
        return transition;
    }

    /** Framework transition listener with no-op defaults. */
    private abstract static class Listener implements Transition.TransitionListener {

        @Override
        public void onTransitionStart(Transition transition) {
        }

        @Override
        public void onTransitionEnd(Transition transition) {
        }

        @Override
        public void onTransitionCancel(Transition transition) {
        }

        @Override
        public void onTransitionPause(Transition transition) {
        }

        @Override
        public void onTransitionResume(Transition transition) {
        }
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
