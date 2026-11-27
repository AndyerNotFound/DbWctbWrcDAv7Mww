package dev.neko.chatdemo.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.transition.TransitionManager;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.transition.MaterialContainerTransform;

import dev.neko.chatdemo.R;

/**
 * Sidebar panel: a collapsing title, the pinned quick entries as one full width column, then the
 * chat history. Tapping a day group grows that card into {@link SidebarGroupFragment} inside this
 * panel — the group page is a child fragment, so expanding and collapsing never swaps pages.
 *
 * <p>A collapse asked for mid expansion is queued and played once the expansion finishes, so the
 * transform always retraces its own path instead of cutting.
 */
public class SidebarFragment extends Fragment {

    private static final String TAG_GROUP = "SidebarGroupFragment";
    private static final String STATE_GROUP = "expanded_group";

    @Nullable
    private Callbacks callbacks;

    @Nullable
    private View root;
    @Nullable
    private View groupHost;
    @Nullable
    private MaterialCardView expandedCard;

    private int expandedGroup = -1;
    private boolean transforming;
    private boolean pendingCollapse;
    private boolean collapsing;
    private int morphToken;

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
        groupHost = view.findViewById(R.id.sidebar_group_host);

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

        if (expandedGroup >= 0) {
            // Rebuilt after a configuration change: restore the open group page without animating.
            showGroupInstantly(expandedGroup);
        }
    }

    @Override
    public void onDestroyView() {
        root = null;
        groupHost = null;
        expandedCard = null;
        super.onDestroyView();
    }

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
            if (group == expandedGroup) {
                card.setVisibility(View.INVISIBLE);
                expandedCard = card;
            }
            int index = group;
            card.setOnClickListener(v -> expand(v, index));
            host.addView(card);
        }
    }

    // ---------------------------------------------------------------- expand / collapse

    /** Grows the tapped card into its day page, inside this panel. */
    private void expand(View card, int group) {
        if (transforming || expandedGroup >= 0 || groupHost == null) {
            return;
        }
        expandedGroup = group;
        expandedCard = (MaterialCardView) card;

        SidebarGroupFragment page = SidebarGroupFragment.newInstance(group);
        getChildFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.sidebar_group_host, page, TAG_GROUP)
                .commitNow();

        // The host is invisible but laid out, so the transform has real end bounds right away.
        morph(card, groupHost, true, () -> {
            card.setVisibility(View.INVISIBLE);
            groupHost.setVisibility(View.VISIBLE);
        });
    }

    /** Shrinks the day page back into the card it came from. */
    public void collapseGroup() {
        if (expandedGroup < 0) {
            return;
        }
        if (transforming) {
            // Let the expansion finish first, then retrace it instead of cutting it.
            pendingCollapse = true;
            return;
        }
        View card = expandedCard;
        if (card == null || groupHost == null || root == null) {
            finishCollapse();
            return;
        }
        // Set before the transform starts: onMorphDone reads it to know this was a collapse.
        collapsing = true;
        morph(groupHost, card, false, () -> {
            card.setVisibility(View.VISIBLE);
            groupHost.setVisibility(View.INVISIBLE);
        });
    }

    private void finishCollapse() {
        if (expandedCard != null) {
            expandedCard.setVisibility(View.VISIBLE);
        }
        if (groupHost != null) {
            groupHost.setVisibility(View.INVISIBLE);
        }
        expandedGroup = -1;
        expandedCard = null;
        collapsing = false;
        removeGroupPage();
    }

    private void removeGroupPage() {
        if (!isAdded()) {
            return;
        }
        Fragment page = getChildFragmentManager().findFragmentByTag(TAG_GROUP);
        if (page != null) {
            getChildFragmentManager().beginTransaction()
                    .setReorderingAllowed(true)
                    .remove(page)
                    .commitAllowingStateLoss();
        }
    }

    private void showGroupInstantly(int group) {
        if (groupHost == null) {
            return;
        }
        SidebarGroupFragment page = SidebarGroupFragment.newInstance(group);
        getChildFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.sidebar_group_host, page, TAG_GROUP)
                .commitNow();
        groupHost.setVisibility(View.VISIBLE);
    }

    /** True while a day page sits on top of the panel, so back closes it first. */
    public boolean isGroupOpen() {
        return expandedGroup >= 0;
    }

    // ---------------------------------------------------------------- transform plumbing

    /**
     * Runs a container transform between two views inside this panel.
     *
     * <p>The bookkeeping is time based: {@code beginDelayedTransition} clones the transition and a
     * clone drops its listeners, so the completion is scheduled from the transform duration
     * instead of a listener. That also gives the queued collapse a well defined moment to play.
     */
    private void morph(View start, View end, boolean entering, Runnable swap) {
        if (root == null) {
            swap.run();
            return;
        }
        MaterialContainerTransform transform =
                new MaterialContainerTransform(requireContext(), entering);
        transform.setDrawingViewId(R.id.sidebar_start_root);
        transform.setScrimColor(Color.TRANSPARENT);
        transform.setAllContainerColors(MaterialColors.getColor(root,
                com.google.android.material.R.attr.colorSurface));
        transform.setStartView(start);
        transform.setEndView(end);
        // A single target keeps the transform off the start view.
        transform.addTarget(end);

        long duration = transform.getDuration() < 0 ? 320 : transform.getDuration();
        final int token = ++morphToken;
        transforming = true;

        TransitionManager.beginDelayedTransition((ViewGroup) root, transform);
        swap.run();

        root.postDelayed(() -> onMorphDone(token), duration + 40);
    }

    private void onMorphDone(int token) {
        if (token != morphToken || !transforming) {
            return;
        }
        transforming = false;
        if (collapsing) {
            // The collapse finished: the group page has served its purpose.
            collapsing = false;
            expandedGroup = -1;
            expandedCard = null;
            removeGroupPage();
        }
        if (pendingCollapse) {
            pendingCollapse = false;
            collapseGroup();
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
