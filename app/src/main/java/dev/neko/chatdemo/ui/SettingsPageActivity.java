package dev.neko.chatdemo.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.transition.platform.MaterialContainerTransform;
import com.google.android.material.transition.platform.MaterialContainerTransformSharedElementCallback;

import dev.neko.chatdemo.R;
import dev.neko.chatdemo.SettingsActivity;

/** Shared chrome for the settings pages: top bar, insets, edge to edge and the page transition. */
public abstract class SettingsPageActivity extends AppCompatActivity {

    private static final String SHARED_CONTAINER = "settings_container";

    protected SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        boolean morphing = usesContainerTransform();
        if (morphing) {
            // Both have to be in place before the content view is installed, otherwise the
            // framework has nothing to map the shared element onto.
            getWindow().requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS);
            setEnterSharedElementCallback(new MaterialContainerTransformSharedElementCallback());
        }

        super.onCreate(savedInstanceState);
        SettingsUi.setupWindow(this);
        prefs = getSharedPreferences(SettingsActivity.PREFS, MODE_PRIVATE);

        setContentView(R.layout.activity_settings);

        if (morphing) {
            findViewById(android.R.id.content).setTransitionName(SHARED_CONTAINER);
            getWindow().setEnterTransition(null);
            getWindow().setSharedElementEnterTransition(containerTransform(true));
            getWindow().setSharedElementReturnTransition(containerTransform(false));
        } else {
            UiTransitions.incoming(this, true);
        }

        SettingsUi.applyInsets(findViewById(R.id.settings_root),
                findViewById(R.id.settings_top_bar), findViewById(R.id.settings_scroll));

        ((TextView) findViewById(R.id.settings_page_title)).setText(titleRes());
        findViewById(R.id.settings_back).setOnClickListener(v -> finish());
        buildContent(findViewById(R.id.settings_content));
    }

    protected abstract int titleRes();

    protected abstract void buildContent(LinearLayout content);

    /** True when the page was opened from a shared element and should morph instead of sliding. */
    protected boolean usesContainerTransform() {
        return getIntent().getBooleanExtra(SettingsActivity.EXTRA_CONTAINER, false);
    }

    private MaterialContainerTransform containerTransform(boolean entering) {
        MaterialContainerTransform transform = new MaterialContainerTransform(this, entering);
        transform.setAllContainerColors(MaterialColors.getColor(
                findViewById(android.R.id.content),
                com.google.android.material.R.attr.colorSurface));
        transform.addTarget(android.R.id.content);
        return transform;
    }
}
