package dev.neko.chatdemo.ui;

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

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import dev.neko.chatdemo.R;

/**
 * One day group, shown inside the sidebar panel. It is a child fragment of
 * {@link SidebarFragment}, so it never replaces the page: the panel's card grows into it and the
 * back control shrinks it back into the same card.
 */
public class SidebarGroupFragment extends Fragment {

    private static final String ARG_GROUP = "group";

    private int group;

    public static SidebarGroupFragment newInstance(int group) {
        SidebarGroupFragment fragment = new SidebarGroupFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_GROUP, group);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        group = args == null ? 0 : args.getInt(ARG_GROUP, 0);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sidebar_group, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View appBar = view.findViewById(R.id.sidebar_group_app_bar);
        View scroll = view.findViewById(R.id.sidebar_group_scroll);
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

        CollapsingToolbarLayout collapsing = view.findViewById(R.id.sidebar_group_collapsing);
        SettingsUi.setTitle(collapsing, getString(SidebarData.titleRes(group)));

        MaterialToolbar toolbar = view.findViewById(R.id.sidebar_group_toolbar);
        toolbar.setNavigationOnClickListener(v -> collapse());

        TextView meta = view.findViewById(R.id.sidebar_group_meta);
        meta.setText(getString(R.string.sidebar_group_count, SidebarData.sessions(group).length));

        buildSessions(view.findViewById(R.id.sidebar_group_list));
    }

    private void collapse() {
        Fragment parent = getParentFragment();
        if (parent instanceof SidebarFragment) {
            ((SidebarFragment) parent).collapseGroup();
        }
    }

    private void buildSessions(LinearLayout host) {
        int spacing = Math.round(4 * getResources().getDisplayMetrics().density);
        int height = Math.round(44 * getResources().getDisplayMetrics().density);

        String[] sessions = SidebarData.sessions(group);
        for (int i = 0; i < sessions.length; i++) {
            MaterialButton button = (MaterialButton) getLayoutInflater()
                    .inflate(R.layout.item_drawer_session, host, false);
            button.setText(sessions[i]);
            button.setCheckable(true);
            button.setChecked(i == 0);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, height);
            lp.topMargin = spacing;
            host.addView(button, lp);
            button.setOnClickListener(v -> {
                for (int child = 0; child < host.getChildCount(); child++) {
                    View item = host.getChildAt(child);
                    if (item instanceof MaterialButton) {
                        ((MaterialButton) item).setChecked(item == button);
                    }
                }
                button.setChecked(true);
            });
        }
    }
}
