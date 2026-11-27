package dev.neko.chatdemo.sheet;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.transition.TransitionManager;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.transition.MaterialSharedAxis;

import dev.neko.chatdemo.R;

/**
 * Attachment / context sheet. Pages are stacked in the same frame and swapped with a shared-axis
 * X transition; the system back key walks back to the root page.
 */
public class AttachmentsSheet extends BottomSheetDialog {

    private final View[] pages;
    private MaterialButton backButton;
    private TextView title;
    private TextView subtitle;
    private boolean onRoot = true;

    public AttachmentsSheet(@NonNull Context context) {
        super(context);

        View content = LayoutInflater.from(context).inflate(R.layout.sheet_attachments, null);
        setContentView(content);

        View pagesContainer = content.findViewById(R.id.sheet_pages);
        pages = new View[]{
                content.findViewById(R.id.page_root),
                content.findViewById(R.id.page_drafts),
                content.findViewById(R.id.page_web),
                content.findViewById(R.id.page_workspace),
                content.findViewById(R.id.page_features),
                content.findViewById(R.id.page_compress)
        };
        backButton = content.findViewById(R.id.sheet_back);
        title = content.findViewById(R.id.sheet_title);
        subtitle = content.findViewById(R.id.sheet_subtitle);
        MaterialButton close = content.findViewById(R.id.sheet_close);

        close.setOnClickListener(v -> dismiss());
        backButton.setOnClickListener(v -> navigateToRoot());

        content.findViewById(R.id.row_image).setOnClickListener(v -> demoAction("Demo: image picker"));
        content.findViewById(R.id.row_video).setOnClickListener(v -> demoAction("Demo: video picker"));
        content.findViewById(R.id.row_file).setOnClickListener(v -> demoAction("Demo: file picker"));
        content.findViewById(R.id.row_camera).setOnClickListener(v -> demoAction("Demo: camera capture"));
        content.findViewById(R.id.row_draft).setOnClickListener(v ->
                showPage(1, "Drafts", "3 saved drafts · local only"));
        content.findViewById(R.id.row_web).setOnClickListener(v ->
                showPage(2, "Web search", "Provider, safety and result budget"));
        content.findViewById(R.id.row_workspace).setOnClickListener(v ->
                showPage(3, "Workspace", "Bound directory and write access"));
        content.findViewById(R.id.row_features).setOnClickListener(v ->
                showPage(4, "Features", "Capabilities folded into this session"));
        content.findViewById(R.id.row_compress).setOnClickListener(v ->
                showPage(5, "Compress history", "Fold older turns into a summary"));

        Slider results = content.findViewById(R.id.web_result_slider);
        TextView resultValue = content.findViewById(R.id.web_result_value);
        results.addOnChangeListener((slider, value, fromUser) ->
                resultValue.setText(String.valueOf((int) value)));

        Slider keep = content.findViewById(R.id.compress_keep_slider);
        TextView keepValue = content.findViewById(R.id.compress_keep_value);
        keep.addOnChangeListener((slider, value, fromUser) ->
                keepValue.setText(String.valueOf((int) value)));

        content.findViewById(R.id.compress_action).setOnClickListener(v ->
                demoAction("Demo: session compressed · 22.4k tokens saved"));

        setOnShowListener(dialog -> {
            BottomSheetBehavior<FrameLayout> behavior = getBehavior();
            behavior.setSkipCollapsed(true);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        });
    }

    private void showPage(int index, String pageTitle, String pageSubtitle) {
        if (index <= 0 || index >= pages.length) {
            navigateToRoot();
            return;
        }
        if (!onRoot) {
            // No nested pages in this demo; reset first so the X transition stays clean.
            for (int i = 1; i < pages.length; i++) {
                pages[i].setVisibility(View.GONE);
            }
        }
        MaterialSharedAxis axis = new MaterialSharedAxis(MaterialSharedAxis.X, true);
        axis.setDuration(320);
        TransitionManager.beginDelayedTransition((android.view.ViewGroup) pages[0].getParent(), axis);
        for (int i = 1; i < pages.length; i++) {
            pages[i].setVisibility(i == index ? View.VISIBLE : View.GONE);
        }
        pages[0].setVisibility(View.GONE);
        backButton.setVisibility(View.VISIBLE);
        title.setText(pageTitle);
        subtitle.setText(pageSubtitle);
        onRoot = false;
    }

    private void navigateToRoot() {
        if (onRoot) {
            return;
        }
        MaterialSharedAxis axis = new MaterialSharedAxis(MaterialSharedAxis.X, false);
        axis.setDuration(320);
        TransitionManager.beginDelayedTransition((android.view.ViewGroup) pages[0].getParent(), axis);
        for (View page : pages) {
            page.setVisibility(View.GONE);
        }
        pages[0].setVisibility(View.VISIBLE);
        backButton.setVisibility(View.INVISIBLE);
        title.setText("Add to chat");
        subtitle.setText("Session 04 · Design folder");
        onRoot = true;
    }

    private void demoAction(String message) {
        Context context = getContext();
        dismiss();
        if (context instanceof Activity) {
            View root = ((Activity) context).findViewById(android.R.id.content);
            Snackbar.make(root, message, Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        if (!onRoot) {
            navigateToRoot();
        } else {
            super.onBackPressed();
        }
    }
}
