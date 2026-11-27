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

import com.google.android.material.button.MaterialButton;

import dev.neko.chatdemo.R;

/**
 * One day group as a full page: the end container of the container transform that starts on a
 * group card in {@link SidebarFragment}. The back control pops back onto the card.
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
        ViewCompat.setTransitionName(view, SidebarFragment.SHARED_GROUP);

        View header = view.findViewById(R.id.sidebar_group_header);
        View scroll = view.findViewById(R.id.sidebar_group_scroll);
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

        TextView title = view.findViewById(R.id.sidebar_group_title);
        title.setText(SidebarData.titleRes(group));

        TextView meta = view.findViewById(R.id.sidebar_group_meta);
        meta.setText(getString(R.string.sidebar_group_count, SidebarData.sessions(group).length));

        MaterialButton back = view.findViewById(R.id.sidebar_group_back);
        back.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        LinearLayout list = view.findViewById(R.id.sidebar_group_list);
        buildSessions(list);
    }

    private void buildSessions(LinearLayout host) {
        int spacing = Math.round(4 * getResources().getDisplayMetrics().density);

        for (String name : SidebarData.sessions(group)) {
            MaterialButton button = (MaterialButton) getLayoutInflater()
                    .inflate(R.layout.item_drawer_session, host, false);
            button.setText(name);
            button.setCheckable(true);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    Math.round(44 * getResources().getDisplayMetrics().density));
            lp.topMargin = spacing;
            host.addView(button, lp);
            button.setOnClickListener(v -> {
                for (int i = 0; i < host.getChildCount(); i++) {
                    View child = host.getChildAt(i);
                    if (child instanceof MaterialButton) {
                        ((MaterialButton) child).setChecked(child == button);
                    }
                }
                button.setChecked(true);
            });
        }
    }
}
