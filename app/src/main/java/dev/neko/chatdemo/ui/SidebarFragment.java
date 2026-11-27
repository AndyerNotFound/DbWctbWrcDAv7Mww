package dev.neko.chatdemo.ui;

import android.content.Context;
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

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.transition.Hold;
import com.google.android.material.transition.MaterialContainerTransform;

import dev.neko.chatdemo.R;

/**
 * Sidebar panel: collapse control and title, the pinned quick entries as one full width column,
 * then the chat history. Each day group is a card, and tapping one grows it into
 * {@link SidebarGroupFragment} with a container transform instead of expanding it in place.
 */
public class SidebarFragment extends Fragment {

    /** Shared element name carried from a group card to the group page. */
    static final String SHARED_GROUP = "sidebar_group_container";

    private static final String STATE_OPEN_GROUP = "open_group";
    private static final String BACK_STACK_GROUP = "SidebarFragment::group";
    private static final String TAG_GROUP = "SidebarGroupFragment";

    private final Hold holdTransition = new Hold();

    /** Group whose page is on top; its card keeps the shared name so the return can land on it. */
    private int openGroup = -1;

    @Nullable
    private Callbacks callbacks;

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
            openGroup = savedInstanceState.getInt(STATE_OPEN_GROUP, -1);
        }
        // Keep the panel on screen while the group page grows over it, and again while it shrinks
        // back, so the morph always has a container underneath.
        holdTransition.addTarget(R.id.sidebar_start_root);
        setExitTransition(holdTransition);
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
        outState.putInt(STATE_OPEN_GROUP, openGroup);
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

        View header = view.findViewById(R.id.sidebar_header);
        View scroll = view.findViewById(R.id.sidebar_scroll);
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            header.setPadding(header.getPaddingLeft(), bars.top,
                    header.getPaddingRight(), header.getPaddingBottom());
            scroll.setPadding(scroll.getPaddingLeft(), scroll.getPaddingTop(),
                    scroll.getPaddingRight(), bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(view);

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
            if (group == openGroup) {
                // Recreated after a pop, so the return transform still has a landing spot: the
                // source name has to match what the transaction recorded.
                ViewCompat.setTransitionName(card, sourceName(group));
            }
            int index = group;
            card.setOnClickListener(v -> openGroupPage(v, index));
            host.addView(card);
        }
    }

    private static String sourceName(int group) {
        return SHARED_GROUP + "_source_" + group;
    }

    /** Grows the tapped card into its day page; the return shrinks the page back onto the card. */
    private void openGroupPage(View card, int group) {
        // Source and target names differ, so only this card takes part in the transform.
        ViewCompat.setTransitionName(card, sourceName(group));
        openGroup = group;

        SidebarGroupFragment page = SidebarGroupFragment.newInstance(group);
        int surface = MaterialColors.getColor(card,
                com.google.android.material.R.attr.colorSurface);

        MaterialContainerTransform enter = new MaterialContainerTransform(requireContext(), true);
        enter.setDrawingViewId(R.id.sidebar_end_root);
        enter.setAllContainerColors(surface);
        page.setSharedElementEnterTransition(enter);
        holdTransition.setDuration(enter.getDuration());

        MaterialContainerTransform back = new MaterialContainerTransform(requireContext(), false);
        back.setDrawingViewId(R.id.sidebar_start_root);
        back.setAllContainerColors(surface);
        page.setSharedElementReturnTransition(back);

        getParentFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .addSharedElement(card, SHARED_GROUP)
                .replace(R.id.sidebar_container, page, TAG_GROUP)
                .addToBackStack(BACK_STACK_GROUP)
                .commit();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
