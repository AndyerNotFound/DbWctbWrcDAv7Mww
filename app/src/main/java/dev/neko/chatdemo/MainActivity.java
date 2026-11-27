package dev.neko.chatdemo;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Locale;

import dev.neko.chatdemo.sheet.AttachmentsSheet;
import dev.neko.chatdemo.sheet.ReasoningSheet;
import dev.neko.chatdemo.ui.ChatFragment;
import dev.neko.chatdemo.ui.FullscreenInputDialog;
import dev.neko.chatdemo.ui.MapFragment;
import dev.neko.chatdemo.ui.MissionFragment;
import dev.neko.chatdemo.ui.UiTransitions;
import dev.neko.chatdemo.ui.UsageFragment;

public class MainActivity extends AppCompatActivity implements ReasoningSheet.Listener {

    private static final int[] STATUS_ICONS = {
            R.drawable.ic_check_circle, R.drawable.ic_schedule, R.drawable.ic_check_circle
    };
    private static final int[] STATUS_TINTS = {
            androidx.appcompat.R.attr.colorPrimary,
            com.google.android.material.R.attr.colorTertiary,
            com.google.android.material.R.attr.colorOutline
    };
    private static final String[] REPLIES = {
            "I folded that into the dock. The six-line cap is in place, and the sheet now walks back with the system key.",
            "Noted. I would keep the split button bound to the reasoning level and let the label carry the model name.",
            "Then the last piece is the screenshot pass: Chat, Map, Mission, Usage, both sheets. Light mode first.",
            "Done. The wavy bar only comes alive while tokens are moving; the determinate fill returns when it settles."
    };

    private String[] tabTitles;
    private String[] folders;
    private String[] presets;
    private String[] sessionStates;
    private String[] interpolations;

    private View root;
    private View topBar;
    private View inputDock;
    private TextInputLayout inputLayout;
    private TextInputEditText input;
    private MaterialButton fullscreenButton;
    private MaterialButton sendButton;
    private MaterialButton recordButton;
    private MaterialButton modelButton;
    private MaterialButton folderButton;
    private MaterialButton sessionButton;
    private MaterialButton presetButton;
    private MaterialButton interpolationButton;
    private LinearProgressIndicator contextProgress;
    private TextView contextLabel;
    private MaterialCardView noticeCard;
    private ImageView noticeIcon;
    private TextView noticeText;
    private MaterialButton noticeAction;
    private Runnable noticeTimeout;
    private ViewPager2 pager;
    private TabLayout tabs;

    private ChatFragment chatFragment;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable replyRunnable;
    private Runnable recordingRunnable;
    private Runnable scrollTicker;

    private boolean generating;
    private boolean recording;
    private boolean fullscreenShown;
    private int contextPercent = 68;
    private int effortIndex = 5;
    private int statusIndex = 0;
    private int presetIndex = 0;
    private int interpolationIndex = 0;
    private int recordSeconds = 0;
    private int replyCursor = 0;
    private int sendBackgroundColor;
    private String modelName = "GPT-5.4";
    private String modelMeta = "OpenAI · 200k context · vision";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        setContentView(R.layout.activity_main);
        loadLabels();

        bindViews();
        updateSystemBarAppearance();
        setupInsets();
        setupTabs();
        setupTopBar();
        setupInput();
        setupActionTicker();
        setGenerating(false);
        updateContextIndicator(false);
        updateModelChip();
        updateInputState();
    }

    private void loadLabels() {
        tabTitles = new String[]{
                getString(R.string.tab_chat), getString(R.string.tab_map),
                getString(R.string.tab_mission), getString(R.string.tab_usage)
        };
        folders = new String[]{
                getString(R.string.folder_design), getString(R.string.folder_research),
                getString(R.string.folder_personal)
        };
        presets = new String[]{
                getString(R.string.preset_balanced), getString(R.string.preset_creative),
                getString(R.string.preset_precise), getString(R.string.preset_debug)
        };
        sessionStates = new String[]{
                getString(R.string.state_active), getString(R.string.state_paused),
                getString(R.string.state_complete)
        };
        interpolations = new String[]{
                getString(R.string.interp_smooth), getString(R.string.interp_linear),
                getString(R.string.interp_adaptive)
        };
    }

    private void bindViews() {
        root = findViewById(R.id.root);
        topBar = findViewById(R.id.top_bar);
        inputDock = findViewById(R.id.input_dock_container);
        inputLayout = findViewById(R.id.input_layout);
        input = findViewById(R.id.input);
        fullscreenButton = findViewById(R.id.btn_fullscreen);
        sendButton = findViewById(R.id.btn_send);
        recordButton = findViewById(R.id.btn_record);
        modelButton = findViewById(R.id.model_button);
        folderButton = findViewById(R.id.folder_button);
        sessionButton = findViewById(R.id.session_button);
        presetButton = findViewById(R.id.btn_preset);
        interpolationButton = findViewById(R.id.btn_interpolation);
        contextProgress = findViewById(R.id.context_progress);
        contextLabel = findViewById(R.id.context_label);
        contextProgress.setWaveAmplitude(0);
        setupNotice();
        pager = findViewById(R.id.pager);
        tabs = findViewById(R.id.tabs);

        sendBackgroundColor = color(com.google.android.material.R.attr.colorPrimaryContainer);
        sendButton.setBackgroundTintList(containerTint(sendBackgroundColor));
    }

    private void updateSystemBarAppearance() {
        boolean night = (getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(!night);
        controller.setAppearanceLightNavigationBars(!night);
    }

    private void newChat() {
        if (chatFragment != null) {
            chatFragment.resetConversation();
        }
        sessionButton.setText(R.string.new_chat_title);
        contextPercent = 4;
        updateContextIndicator(true);
        showNotice(R.drawable.ic_add_comment, getString(R.string.notice_new_chat));
    }

    private void setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            topBar.setPadding(bars.left, bars.top, bars.right, topBar.getPaddingBottom());
            inputDock.setPadding(bars.left, 0, bars.right, Math.max(bars.bottom, ime.bottom));
            if (noticeCard != null) {
                FrameLayout.LayoutParams params =
                        (FrameLayout.LayoutParams) noticeCard.getLayoutParams();
                int top = bars.top + dp(6);
                if (params.topMargin != top) {
                    params.topMargin = top;
                    noticeCard.setLayoutParams(params);
                }
            }
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
    }

    private void setupTabs() {
        final Fragment[] pages = new Fragment[]{
                new ChatFragment(), new MapFragment(), new MissionFragment(), new UsageFragment()
        };
        chatFragment = (ChatFragment) pages[0];

        pager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                return pages[position];
            }

            @Override
            public int getItemCount() {
                return pages.length;
            }
        });
        pager.setOffscreenPageLimit(3);
        new TabLayoutMediator(tabs, pager, (tab, position) ->
                tab.setText(tabTitles[position])).attach();

        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                hideKeyboard();
                input.clearFocus();
            }
        });
    }

    private void setupTopBar() {
        findViewById(R.id.btn_sidebar).setOnClickListener(v ->
                UiTransitions.startPanel(this, new Intent(this, SidebarActivity.class)));

        folderButton.setOnClickListener(v -> showFolderMenu(v));

        sessionButton.setOnClickListener(v -> {
            statusIndex = (statusIndex + 1) % sessionStates.length;
            sessionButton.setIconResource(STATUS_ICONS[statusIndex]);
            sessionButton.setIconTint(ColorStateList.valueOf(color(STATUS_TINTS[statusIndex])));
            showNotice(STATUS_ICONS[statusIndex],
                    getString(R.string.notice_session_state, sessionStates[statusIndex]));
        });

        presetButton.setOnClickListener(this::showPresetMenu);
        findViewById(R.id.btn_new_chat).setOnClickListener(v -> newChat());
        findViewById(R.id.btn_menu).setOnClickListener(this::showSessionMenu);

    }

    private void showFolderMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor, Gravity.END);
        for (int i = 0; i < folders.length; i++) {
            popup.getMenu().add(0, i, i, folders[i]);
        }
        popup.setOnMenuItemClickListener(item -> {
            String folder = folders[item.getItemId() % folders.length];
            folderButton.setText(folder);
            showNotice(R.drawable.ic_folder, getString(R.string.notice_folder, folder));
            return true;
        });
        popup.show();
    }

    private void showPresetMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor, Gravity.END);
        popup.getMenu().setGroupCheckable(0, true, true);
        for (int i = 0; i < presets.length; i++) {
            popup.getMenu().add(0, i, i, presets[i])
                    .setCheckable(true)
                    .setChecked(i == presetIndex);
        }
        popup.setOnMenuItemClickListener(item -> {
            presetIndex = item.getItemId();
            int tint = presetIndex == 1
                    ? com.google.android.material.R.attr.colorTertiary
                    : presetIndex == 2
                    ? androidx.appcompat.R.attr.colorPrimary
                    : com.google.android.material.R.attr.colorOnSurfaceVariant;
            presetButton.setIconTint(ColorStateList.valueOf(color(tint)));
            showNotice(R.drawable.ic_tune, getString(R.string.notice_preset, presets[presetIndex]));
            return true;
        });
        popup.show();
    }

    private void showSessionMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor, Gravity.END);
        popup.getMenu().add(0, 0, 0, getString(R.string.menu_rename));
        popup.getMenu().add(0, 1, 1, getString(R.string.menu_export));
        popup.getMenu().add(0, 2, 2, getString(R.string.menu_compress));
        popup.getMenu().add(0, 3, 3, getString(R.string.menu_settings));
        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 2:
                    new AttachmentsSheet(this).show();
                    break;
                case 3:
                    SettingsActivity.start(this);
                    break;
                default:
                    showNotice(R.drawable.ic_more_vert,
                            getString(R.string.notice_menu_demo, item.getTitle()));
                    break;
            }
            return true;
        });
        popup.show();
    }

    private void setupInput() {
        if (fullscreenButton != null) {
            fullscreenButton.setOnClickListener(v -> openFullscreenInput());
        }

        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                inputLayout.setError(null);
                if (s.length() == 0) {
                    // The layout only re-measures the field on the next layout pass after the
                    // text collapses back to a single line. Without this nudge the dock can keep
                    // the height of the deleted paragraph.
                    input.post(() -> {
                        input.setMinHeight(0);
                        input.requestLayout();
                        inputLayout.requestLayout();
                    });
                }
                updateInputState();
            }
        });

        sendButton.setOnClickListener(v -> {
            if (generating) {
                stopGenerating();
            } else {
                sendCurrentInput();
            }
        });

        recordButton.addOnCheckedChangeListener((button, isChecked) -> setRecording(isChecked));

        findViewById(R.id.btn_attach).setOnClickListener(v -> new AttachmentsSheet(this).show());
        findViewById(R.id.btn_quick).setOnClickListener(v -> {
            String quick = "Walk me through the layout hierarchy of the input dock — ";
            input.setText(quick);
            input.setSelection(quick.length());
            input.requestFocus();
            showNotice(R.drawable.ic_bolt, getString(R.string.notice_quick));
        });
        modelButton.setOnClickListener(v -> openReasoning(false));
        interpolationButton.setOnClickListener(v -> {
            interpolationIndex = (interpolationIndex + 1) % interpolations.length;
            int tint = interpolationIndex == 2
                    ? com.google.android.material.R.attr.colorTertiary
                    : com.google.android.material.R.attr.colorOnSurfaceVariant;
            interpolationButton.setIconTint(ColorStateList.valueOf(color(tint)));
            showNotice(R.drawable.ic_animation,
                    getString(R.string.notice_interpolation, interpolations[interpolationIndex]));
        });
        interpolationButton.setTooltipText(getString(R.string.cd_interpolation));
        recordButton.setTooltipText(getString(R.string.cd_record));
        sendButton.setTooltipText(getString(R.string.cd_send));
    }

    private void updateInputState() {
        boolean hasText = input.getText() != null
                && input.getText().toString().trim().length() > 0;
        if (fullscreenButton != null && hasText != fullscreenShown) {
            fullscreenShown = hasText;
            fullscreenButton.animate().cancel();
            fullscreenButton.animate()
                    .alpha(hasText ? 1f : 0f)
                    .setDuration(150)
                    .start();
            fullscreenButton.setClickable(hasText);
            fullscreenButton.setFocusable(hasText);
            fullscreenButton.setImportantForAccessibility(hasText
                    ? View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
                    : View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        }
    }

    private void openReasoning(boolean models) {
        ReasoningSheet.newInstance(models, modelName, modelMeta, effortIndex)
                .show(getSupportFragmentManager(), "reasoning");
    }

    @Override
    public void onModelSelected(@NonNull String name, @NonNull String meta) {
        modelName = name;
        modelMeta = meta;
        if (modelButton != null) {
            modelButton.setText(compactModelName(name));
        }
    }

    @Override
    public void onEffortSelected(int index, String budgetLabel) {
        effortIndex = index;
        updateModelChip();
    }

    private void openFullscreenInput() {
        String initial = input.getText() == null ? "" : input.getText().toString();
        new FullscreenInputDialog(this, initial, new FullscreenInputDialog.Listener() {
            @Override
            public void onSend(String text) {
                input.setText(text);
                input.setSelection(input.getText() == null ? 0 : input.getText().length());
                sendCurrentInput();
            }

            @Override
            public void onClosed(String text) {
                input.setText(text);
                if (input.getText() != null) {
                    input.setSelection(input.getText().length());
                }
                updateInputState();
            }
        }).showWithKeyboard();
    }

    private void sendCurrentInput() {
        if (generating) {
            stopGenerating();
            return;
        }
        String text = input.getText() == null ? "" : input.getText().toString().trim();
        if (text.isEmpty()) {
            showNotice(R.drawable.ic_error, getString(R.string.notice_empty_input));
            input.requestFocus();
            return;
        }

        if (chatFragment != null) {
            chatFragment.addUserMessage(text);
        }
        input.setText("");
        updateInputState();
        setGenerating(true);
        handler.removeCallbacks(replyRunnable);
        handler.postDelayed(replyRunnable, 2700);
    }

    private void stopGenerating() {
        handler.removeCallbacks(replyRunnable);
        handler.removeCallbacks(scrollTicker);
        setGenerating(false);
        if (chatFragment != null) {
            chatFragment.addAssistantMessage("Stopped where you asked. The partial thought is still in the thread.");
        }
        showNotice(R.drawable.ic_stop, getString(R.string.notice_stopped));
    }

    private void setGenerating(boolean value) {
        generating = value;
        if (value) {
            contextProgress.setWaveAmplitude((int) (2.5f * getResources().getDisplayMetrics().density));
        }
        contextProgress.setIndeterminate(value);
        if (!value) {
            contextProgress.setWaveAmplitude(0);
            contextProgress.setProgressCompat(contextPercent, true);
            handler.removeCallbacks(scrollTicker);
        } else {
            handler.removeCallbacks(scrollTicker);
            handler.post(scrollTicker);
        }
        updateSendVisual(value);
        updateContextIndicator(false);
        updateInputState();
    }

    private void setupActionTicker() {
        replyRunnable = () -> {
            if (!generating) {
                return;
            }
            String reply = REPLIES[replyCursor % REPLIES.length];
            replyCursor++;
            if (chatFragment != null) {
                chatFragment.addAssistantMessage(reply);
            }
            contextPercent = Math.min(99, contextPercent + 1);
            setGenerating(false);
            showNotice(R.drawable.ic_psychology,
                    getString(R.string.notice_answered, contextPercent));
        };

        scrollTicker = new Runnable() {
            @Override
            public void run() {
                if (!generating) {
                    return;
                }
                if (chatFragment != null) {
                    chatFragment.scrollToBottom();
                }
                handler.postDelayed(this, 900);
            }
        };

        recordingRunnable = new Runnable() {
            @Override
            public void run() {
                if (!recording) {
                    return;
                }
                recordSeconds++;
                updateContextIndicator(false);
                handler.postDelayed(this, 1000);
            }
        };
    }

    private void updateSendVisual(boolean on) {
        if (sendButton == null) {
            return;
        }
        int targetBg = color(on
                ? com.google.android.material.R.attr.colorTertiaryContainer
                : com.google.android.material.R.attr.colorPrimaryContainer);
        int targetIcon = color(on
                ? com.google.android.material.R.attr.colorOnTertiaryContainer
                : com.google.android.material.R.attr.colorOnPrimaryContainer);

        ValueAnimator animator = ValueAnimator.ofArgb(sendBackgroundColor, targetBg);
        animator.setDuration(320);
        animator.addUpdateListener(a -> {
            sendBackgroundColor = ((Number) a.getAnimatedValue()).intValue();
            // Rebuild the state list on every frame so the pressed/disabled states keep the
            // Material state layer instead of being flattened into one solid colour.
            sendButton.setBackgroundTintList(containerTint(sendBackgroundColor));
        });
        animator.start();

        sendButton.setIconResource(on ? R.drawable.ic_stop : R.drawable.ic_send);
        sendButton.setIconTint(iconTint(targetIcon));
        sendButton.setContentDescription(getString(on ? R.string.cd_stop : R.string.cd_send));
        sendButton.animate().scaleX(0.88f).scaleY(0.88f).setDuration(90)
                .withEndAction(() -> sendButton.animate().scaleX(1f).scaleY(1f).setDuration(150).start())
                .start();
    }

    private void updateContextIndicator(boolean animate) {
        if (!generating) {
            contextProgress.setProgressCompat(contextPercent, animate);
        }
        if (recording) {
            int minutes = recordSeconds / 60;
            int seconds = recordSeconds % 60;
            contextLabel.setText(String.format(Locale.US, "REC %02d:%02d", minutes, seconds));
            contextLabel.setTextColor(color(androidx.appcompat.R.attr.colorError));
        } else if (generating) {
            contextLabel.setText(R.string.state_writing);
            contextLabel.setTextColor(color(com.google.android.material.R.attr.colorTertiary));
        } else {
            contextLabel.setText(contextPercent + "%");
            contextLabel.setTextColor(color(com.google.android.material.R.attr.colorOnSurfaceVariant));
        }
    }

    private void updateModelChip() {
        if (modelButton == null) {
            return;
        }
        modelButton.setIconResource(ReasoningSheet.iconForEffort(effortIndex));
    }

    private void setRecording(boolean value) {
        recording = value;
        handler.removeCallbacks(recordingRunnable);
        if (value) {
            recordSeconds = 0;
            handler.post(recordingRunnable);
            showNotice(R.drawable.ic_mic, getString(R.string.notice_recording));
        } else {
            showNotice(R.drawable.ic_mic, getString(R.string.notice_recording_discarded));
        }
        updateContextIndicator(false);
    }

    private String compactModelName(String name) {
        if (name.length() <= 8) {
            return name;
        }
        switch (name) {
            case "Claude Sonnet 4.5":
                return "Sonnet";
            case "Claude Opus 4.5":
                return "Opus";
            case "Gemini 3 Flash":
                return "Gemini";
            case "GPT-5.4 mini":
                return "5.4 mini";
            case "GPT-5.4 Pro":
                return "5.4 Pro";
            default:
                return name.substring(0, Math.min(8, name.length()));
        }
    }

    /** Resolves a colour from the current theme so Material You rolls through everywhere. */
    private int color(@AttrRes int attr) {
        return MaterialColors.getColor(root, attr);
    }

    /**
     * Container tint that keeps the Material state layer alive: pressed darkens, disabled fades.
     * A flat ColorStateList makes a tonal button look and feel static.
     */
    private ColorStateList containerTint(int base) {
        int onSurface = color(com.google.android.material.R.attr.colorOnSurface);
        return new ColorStateList(
                new int[][]{
                        new int[]{-android.R.attr.state_enabled},
                        new int[]{android.R.attr.state_pressed},
                        new int[]{}
                },
                new int[]{
                        ColorUtils.setAlphaComponent(onSurface, 0x1F),
                        MaterialColors.layer(base, onSurface, 0.10f),
                        base
                });
    }

    private ColorStateList iconTint(int base) {
        int onSurface = color(com.google.android.material.R.attr.colorOnSurface);
        return new ColorStateList(
                new int[][]{
                        new int[]{-android.R.attr.state_enabled},
                        new int[]{}
                },
                new int[]{
                        ColorUtils.setAlphaComponent(onSurface, 0x61),
                        base
                });
    }

    private void setupNotice() {
        noticeCard = findViewById(R.id.top_notice);
        noticeIcon = findViewById(R.id.notice_icon);
        noticeText = findViewById(R.id.notice_text);
        noticeAction = findViewById(R.id.notice_action);
        noticeTimeout = this::hideNotice;

        final float[] startY = new float[1];
        final boolean[] dragging = new boolean[1];
        final VelocityTracker[] tracker = new VelocityTracker[1];
        noticeCard.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    startY[0] = event.getRawY();
                    dragging[0] = true;
                    v.animate().cancel();
                    handler.removeCallbacks(noticeTimeout);
                    tracker[0] = VelocityTracker.obtain();
                    tracker[0].addMovement(event);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (!dragging[0]) {
                        return false;
                    }
                    if (tracker[0] != null) {
                        tracker[0].addMovement(event);
                    }
                    // Only upward drags peel the card away from the top edge.
                    v.setTranslationY(Math.min(0f, event.getRawY() - startY[0]));
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (!dragging[0]) {
                        return false;
                    }
                    dragging[0] = false;
                    float velocity = 0f;
                    if (tracker[0] != null) {
                        tracker[0].addMovement(event);
                        tracker[0].computeCurrentVelocity(1000);
                        velocity = tracker[0].getYVelocity();
                        tracker[0].recycle();
                        tracker[0] = null;
                    }
                    if (v.getTranslationY() < -v.getHeight() * 0.3f || velocity < -900f) {
                        hideNotice();
                    } else {
                        v.animate().translationY(0f).setDuration(160).start();
                        handler.postDelayed(noticeTimeout, 2200);
                    }
                    return true;
                default:
                    return false;
            }
        });
    }

    private void showNotice(int iconRes, String message) {
        showNotice(iconRes, message, getString(R.string.notice_dismiss), null);
    }

    private void showNotice(int iconRes, String message, String actionLabel, Runnable action) {
        if (noticeCard == null) {
            return;
        }
        handler.removeCallbacks(noticeTimeout);
        noticeCard.animate().cancel();

        noticeIcon.setImageResource(iconRes);
        noticeText.setText(message);
        noticeAction.setText(actionLabel);
        noticeAction.setOnClickListener(v -> {
            hideNotice();
            if (action != null) {
                action.run();
            }
        });

        noticeCard.setVisibility(View.VISIBLE);
        noticeCard.setAlpha(0f);
        noticeCard.setTranslationY(-noticeCard.getHeight() - dp(16));
        noticeCard.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(220)
                .setInterpolator(new DecelerateInterpolator())
                .start();
        handler.postDelayed(noticeTimeout, 3800);
    }

    private void hideNotice() {
        if (noticeCard == null || noticeCard.getVisibility() != View.VISIBLE) {
            return;
        }
        handler.removeCallbacks(noticeTimeout);
        noticeCard.animate().cancel();
        noticeCard.animate()
                .alpha(0f)
                .translationY(-noticeCard.getHeight() - dp(16))
                .setDuration(180)
                .setInterpolator(new AccelerateInterpolator())
                .withEndAction(() -> {
                    if (noticeCard.getAlpha() == 0f) {
                        noticeCard.setVisibility(View.INVISIBLE);
                    }
                })
                .start();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void hideKeyboard() {
        View focus = getCurrentFocus();
        if (focus == null) {
            return;
        }
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
        }
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
