package dev.neko.chatdemo.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.neko.chatdemo.R;

/**
 * Sidebar panel: a collapsing title, the pinned quick entries as one full width column, then the
 * chat history.
 *
 * <p>The history is an accordion of plain views: each day group is a card that owns an indented
 * session list, so tapping a group opens it where it sits. The list grows from zero to its measured
 * height while its rows settle in from the indent, and the groups below slide down to make room —
 * nothing leaves this page, no activity, no child page, no page swap.
 *
 * <p>One group is open at a time; opening another closes the first in the same frame, so the list
 * only ever travels once.
 */
public class SidebarFragment extends Fragment {

    private static final String STATE_GROUP = "expanded_group";
    private static final long OPEN_MS = 260L;
    private static final long CLOSE_MS = 200L;

    @Nullable
    private Callbacks callbacks;

    @Nullable
    private View root;

    private final List<MaterialCardView> cards = new ArrayList<>();
    private final Map<View, ValueAnimator> heightAnimators = new HashMap<>();

    private int expandedGroup = -1;

    public interface Callbacks {
        void onCollapseSidebar();

        void onNewSession();

        void onSearch();

        void onFolders();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            expandedGroup = savedInstanceState.getInt(STATE_GROUP, -1);
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        callbacks = context instanceof Callbacks ? (Callbacks) context : null;
    }

    @Override
    public void onDetach() {
        callbacks = null;
        super.onDetach();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_GROUP, expandedGroup);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sidebar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        root = view;

        View appBar = view.findViewById(R.id.sidebar_app_bar);
        View scroll = view.findViewById(R.id.sidebar_scroll);
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            appBar.setPadding(appBar.getPaddingLeft(), bars.top,
                    appBar.getPaddingRight(), appBar.getPaddingBottom());
            scroll.setPadding(scroll.getPaddingLeft(), scroll.getPaddingTop(),
                    scroll.getPaddingRight(), bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(view);

        CollapsingToolbarLayout collapsing = view.findViewById(R.id.sidebar_collapsing);
        SettingsUi.setTitle(collapsing, getString(R.string.sidebar_title));

        MaterialToolbar toolbar = view.findViewById(R.id.sidebar_toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            if (callbacks != null) {
                callbacks.onCollapseSidebar();
            }
        });

        view.findViewById(R.id.sidebar_function_new).setOnClickListener(v -> {
            if (callbacks != null) {
                callbacks.onNewSession();
            }
        });
        view.findViewById(R.id.sidebar_function_search).setOnClickListener(v -> {
            if (callbacks != null) {
                callbacks.onSearch();
            }
        });
        view.findViewById(R.id.sidebar_function_folders).setOnClickListener(v -> {
            if (callbacks != null) {
                callbacks.onFolders();
            }
        });

        buildHistory(view.findViewById(R.id.sidebar_history));

        if (expandedGroup >= 0 && expandedGroup < cards.size()) {
            // Rebuilt after a configuration change: keep the open group open, without animating.
            applyOpen(cards.get(expandedGroup), true);
        }
    }

    @Override
    public void onDestroyView() {
        // Copy first: cancelling an animator removes it from the map through its own listener.
        for (ValueAnimator animator : new ArrayList<>(heightAnimators.values())) {
            animator.cancel();
        }
        heightAnimators.clear();
        cards.clear();
        root = null;
        super.onDestroyView();
    }

    // ---------------------------------------------------------------- history

    private void buildHistory(LinearLayout host) {
        int muted = MaterialColors.getColor(host,
                com.google.android.material.R.attr.colorOnSurfaceVariant);

        TextView heading = new TextView(requireContext());
        heading.setText(R.string.sidebar_history);
        heading.setTextSize(12);
        heading.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        heading.setTextColor(muted);
        heading.setPadding(dp(4), dp(2), dp(4), dp(6));
        host.addView(heading);

        for (int group = 0; group < SidebarData.groupCount(); group++) {
            MaterialCardView card = (MaterialCardView) getLayoutInflater()
                    .inflate(R.layout.item_sidebar_group, host, false);
            ((TextView) card.findViewById(R.id.group_label)).setText(SidebarData.titleRes(group));
            ((TextView) card.findViewById(R.id.group_count)).setText(
                    getString(R.string.sidebar_group_count, SidebarData.sessions(group).length));

            buildSessions((LinearLayout) card.findViewById(R.id.group_sessions), group);

            int index = group;
            card.findViewById(R.id.group_header).setOnClickListener(v -> toggle(index));
            host.addView(card);
            cards.add(card);
        }

        // The newest session in the newest group starts out as the active one.
        if (!cards.isEmpty()) {
            View list = cards.get(0).findViewById(R.id.group_sessions);
            if (list instanceof LinearLayout && ((LinearLayout) list).getChildCount() > 0) {
                ((LinearLayout) list).getChildAt(0).setActivated(true);
            }
        }
    }

    /** Fills one group's indented list; the rows are children of the group card, not of the page. */
    private void buildSessions(LinearLayout host, int group) {
        String[] sessions = SidebarData.sessions(group);
        for (int i = 0; i < sessions.length; i++) {
            MaterialButton button = (MaterialButton) getLayoutInflater()
                    .inflate(R.layout.item_drawer_session, host, false);
            button.setText(sessions[i]);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(38));
            lp.topMargin = dp(2);
            host.addView(button, lp);
            button.setOnClickListener(v -> select(button));
        }
    }

    /** Keeps a single session active across the whole history. */
    private void select(MaterialButton button) {
        for (MaterialCardView card : cards) {
            View list = card.findViewById(R.id.group_sessions);
            if (!(list instanceof LinearLayout)) {
                continue;
            }
            LinearLayout group = (LinearLayout) list;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                if (child instanceof MaterialButton) {
                    child.setActivated(child == button);
                }
            }
        }
    }

    // ---------------------------------------------------------------- expand / collapse

    /** Opens the tapped group in place, or closes it when it is already the open one. */
    private void toggle(int group) {
        if (group < 0 || group >= cards.size()) {
            return;
        }
        if (expandedGroup == group) {
            expandedGroup = -1;
            animateOpen(cards.get(group), false);
            return;
        }
        if (expandedGroup >= 0 && expandedGroup < cards.size()) {
            // The old group gives up its space in the same frame, so the list travels once.
            animateOpen(cards.get(expandedGroup), false);
        }
        expandedGroup = group;
        MaterialCardView card = cards.get(group);
        animateOpen(card, true);
        ensureVisible(card);
    }

    /**
     * Nudges the history so the freshly opened group is not left half under the fold: the card
     * grows downward, and the list only has to travel by the part that is still off screen.
     */
    private void ensureVisible(View card) {
        View scroll = root == null ? null : root.findViewById(R.id.sidebar_scroll);
        if (!(scroll instanceof androidx.core.widget.NestedScrollView)) {
            return;
        }
        final androidx.core.widget.NestedScrollView view =
                (androidx.core.widget.NestedScrollView) scroll;
        view.postDelayed(() -> {
            if (!isAdded() || card.getParent() == null) {
                return;
            }
            // Distance from the card to the scroll view's content top.
            int top = card.getTop();
            View parent = (View) card.getParent();
            while (parent != null && parent != view) {
                top += parent.getTop();
                parent = (View) parent.getParent();
            }
            int bottom = top + card.getHeight();
            int visible = view.getScrollY() + view.getHeight() - dp(96);
            if (bottom > visible) {
                view.smoothScrollBy(0, Math.min(bottom - visible, dp(180)));
            }
        }, OPEN_MS + 40);
    }

    /** Closes whichever group is open, from a back press. */
    public void collapseGroup() {
        if (expandedGroup < 0 || expandedGroup >= cards.size()) {
            return;
        }
        MaterialCardView card = cards.get(expandedGroup);
        expandedGroup = -1;
        animateOpen(card, false);
    }

    /** True while a group is open, so back closes it before it closes the panel. */
    public boolean isGroupOpen() {
        return expandedGroup >= 0;
    }

    /**
     * Grows or shrinks one group's session list to its real height.
     *
     * <p>A height animation on the list, not a transition on the card: the card keeps laying out
     * every frame, so the groups underneath follow instead of being revealed from behind a clip.
     * The rows also start pushed in by the indent and settle to their own left edge, which is what
     * makes the nesting read.
     */
    private void animateOpen(MaterialCardView card, final boolean open) {
        View found = card.findViewById(R.id.group_sessions);
        ImageView chevron = card.findViewById(R.id.group_chevron);
        if (!(found instanceof LinearLayout)) {
            return;
        }
        final LinearLayout sessions = (LinearLayout) found;

        if (chevron != null) {
            chevron.animate()
                    .rotation(open ? 180f : 0f)
                    .setDuration(open ? OPEN_MS : CLOSE_MS)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }

        cancelHeightAnimator(sessions);

        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) sessions.getLayoutParams();
        // A hidden list keeps its old height, so only a visible one contributes a start value:
        // a fresh open always unfolds from zero.
        int start = sessions.getVisibility() == View.VISIBLE ? sessions.getHeight() : 0;
        int target;

        if (open) {
            sessions.setVisibility(View.VISIBLE);
            sessions.measure(View.MeasureSpec.makeMeasureSpec(widthOf(sessions),
                            View.MeasureSpec.AT_MOST),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            target = sessions.getMeasuredHeight();
            if (start <= 0) {
                // Start flat so the card unfolds out of the header row.
                lp.height = 0;
                sessions.setLayoutParams(lp);
                start = 0;
            }
            for (int i = 0; i < sessions.getChildCount(); i++) {
                View child = sessions.getChildAt(i);
                child.animate().cancel();
                child.setAlpha(0f);
                child.setTranslationX(-dp(12));
                child.animate()
                        .alpha(1f)
                        .translationX(0f)
                        .setStartDelay(70L + i * 30L)
                        .setDuration(200L)
                        .setInterpolator(new DecelerateInterpolator())
                        .start();
            }
        } else {
            target = 0;
            // A close can land mid-open; drop the row stagger so nothing keeps fading out.
            for (int i = 0; i < sessions.getChildCount(); i++) {
                View child = sessions.getChildAt(i);
                child.animate().cancel();
                child.setAlpha(1f);
                child.setTranslationX(0f);
            }
        }

        if (start == target) {
            settle(sessions, open);
            return;
        }

        final ValueAnimator animator = ValueAnimator.ofInt(start, target);
        animator.setDuration(open ? OPEN_MS : CLOSE_MS);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            LinearLayout.LayoutParams params =
                    (LinearLayout.LayoutParams) sessions.getLayoutParams();
            params.height = (int) animation.getAnimatedValue();
            sessions.setLayoutParams(params);
        });
        animator.addListener(new AnimatorListenerAdapter() {
            private boolean cancelled;

            @Override
            public void onAnimationCancel(Animator animation) {
                cancelled = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                heightAnimators.remove(sessions);
                if (cancelled) {
                    // A newer run owns this list now; leave its frame alone.
                    return;
                }
                settle(sessions, open);
            }
        });
        heightAnimators.put(sessions, animator);
        animator.start();
    }

    /** Leaves the list at its natural height, hidden when closed. */
    private void settle(LinearLayout sessions, boolean open) {
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) sessions.getLayoutParams();
        lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        sessions.setLayoutParams(lp);
        sessions.setVisibility(open ? View.VISIBLE : View.GONE);
    }

    /** Applies an open or closed group without animating, for state restore. */
    private void applyOpen(MaterialCardView card, boolean open) {
        View found = card.findViewById(R.id.group_sessions);
        View chevron = card.findViewById(R.id.group_chevron);
        if (!(found instanceof LinearLayout)) {
            return;
        }
        LinearLayout sessions = (LinearLayout) found;
        cancelHeightAnimator(sessions);
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) sessions.getLayoutParams();
        lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        sessions.setLayoutParams(lp);
        sessions.setVisibility(open ? View.VISIBLE : View.GONE);
        resetRows(sessions);
        if (chevron != null) {
            chevron.setRotation(open ? 180f : 0f);
        }
    }

    private void resetRows(LinearLayout sessions) {
        for (int i = 0; i < sessions.getChildCount(); i++) {
            View child = sessions.getChildAt(i);
            child.animate().cancel();
            child.setAlpha(1f);
            child.setTranslationX(0f);
        }
    }

    private void cancelHeightAnimator(View list) {
        ValueAnimator running = heightAnimators.remove(list);
        if (running != null) {
            running.cancel();
        }
    }

    private int widthOf(View view) {
        int width = view.getWidth();
        if (width > 0) {
            return width;
        }
        View parent = (View) view.getParent();
        if (parent != null && parent.getWidth() > 0) {
            return parent.getWidth();
        }
        return getResources().getDisplayMetrics().widthPixels;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
