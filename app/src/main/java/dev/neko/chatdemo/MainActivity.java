package dev.neko.chatdemo;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialSplitButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Locale;

import dev.neko.chatdemo.sheet.AttachmentsSheet;
import dev.neko.chatdemo.sheet.ReasoningSheet;
import dev.neko.chatdemo.sheet.SidebarSheet;
import dev.neko.chatdemo.ui.ChatFragment;
import dev.neko.chatdemo.ui.FullscreenInputDialog;
import dev.neko.chatdemo.ui.MapFragment;
import dev.neko.chatdemo.ui.MissionFragment;
import dev.neko.chatdemo.ui.UsageFragment;

public class MainActivity extends AppCompatActivity {

    private static final String[] TAB_TITLES = {"Chat", "Map", "Mission", "Usage"};
    private static final String[] FOLDERS = {"Design", "Research", "Personal"};
    private static final String[] PRESETS = {"Balanced", "Creative", "Precise", "Debug"};
    private static final String[] STATUS = {"Active", "Paused", "Complete"};
    private static final int[] STATUS_ICONS = {
            R.drawable.ic_check_circle, R.drawable.ic_schedule, R.drawable.ic_check_circle
    };
    private static final int[] STATUS_TINTS = {
            R.color.neko_primary, R.color.neko_tertiary, R.color.neko_outline
    };
    private static final String[] INTERPOLATIONS = {"Smooth", "Linear", "Adaptive"};
    private static final String[] REPLIES = {
            "I folded that into the dock. The six-line cap is in place, and the sheet now walks back with the system key.",
            "Noted. I would keep the split button bound to the reasoning level and let the label carry the model name.",
            "Then the last piece is the screenshot pass: Chat, Map, Mission, Usage, both sheets. Light mode first.",
            "Done. The wavy bar only comes alive while tokens are moving; the determinate fill returns when it settles."
    };

    private View root;
    private View topBar;
    private View inputDock;
    private TextInputLayout inputLayout;
    private TextInputEditText input;
    private MaterialButton sendButton;
    private MaterialButton recordButton;
    private MaterialButton modelButton;
    private MaterialButton modelChevron;
    private MaterialButton folderButton;
    private MaterialButton folderChevron;
    private MaterialButton folderLabel;
    private MaterialButton sessionButton;
    private MaterialButton presetButton;
    private MaterialButton interpolationButton;
    private LinearProgressIndicator contextProgress;
    private android.widget.TextView contextLabel;
    private ViewPager2 pager;
    private TabLayout tabs;

    private ChatFragment chatFragment;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable replyRunnable;
    private Runnable recordingRunnable;
    private Runnable scrollTicker;

    private boolean generating;
    private boolean recording;
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

    private final ReasoningSheet.Listener reasoningListener = new ReasoningSheet.Listener() {
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
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        setContentView(R.layout.activity_main);

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

    private void bindViews() {
        root = findViewById(R.id.root);
        topBar = findViewById(R.id.top_bar);
        inputDock = findViewById(R.id.input_dock_container);
        inputLayout = findViewById(R.id.input_layout);
        input = findViewById(R.id.input);
        sendButton = findViewById(R.id.btn_send);
        recordButton = findViewById(R.id.btn_record);
        modelButton = findViewById(R.id.model_button);
        modelChevron = findViewById(R.id.model_chevron);
        folderButton = findViewById(R.id.folder_button);
        folderChevron = findViewById(R.id.folder_chevron);
        folderLabel = findViewById(R.id.folder_button);
        sessionButton = findViewById(R.id.session_button);
        presetButton = findViewById(R.id.btn_preset);
        interpolationButton = findViewById(R.id.btn_interpolation);
        contextProgress = findViewById(R.id.context_progress);
        contextLabel = findViewById(R.id.context_label);
        contextProgress.setWaveAmplitude(0);
        pager = findViewById(R.id.pager);
        tabs = findViewById(R.id.tabs);

        sendBackgroundColor = ContextCompat.getColor(this, R.color.neko_primary_container);
        sendButton.setBackgroundTintList(ColorStateList.valueOf(sendBackgroundColor));
        modelButton.setMaxWidth((int) (96 * getResources().getDisplayMetrics().density));
    }

    private void updateSystemBarAppearance() {
        boolean night = (getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(!night);
        controller.setAppearanceLightNavigationBars(!night);
    }

    private void setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            topBar.setPadding(bars.left, bars.top, bars.right, topBar.getPaddingBottom());
            inputDock.setPadding(bars.left, 0, bars.right, Math.max(bars.bottom, ime.bottom));
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
                tab.setText(TAB_TITLES[position])).attach();

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
                new SidebarSheet(this).show());

        View.OnClickListener folderMenu = v -> showFolderMenu(v);
        folderButton.setOnClickListener(folderMenu);
        folderChevron.setOnClickListener(folderMenu);

        sessionButton.setOnClickListener(v -> {
            statusIndex = (statusIndex + 1) % STATUS.length;
            sessionButton.setIconResource(STATUS_ICONS[statusIndex]);
            sessionButton.setIconTint(ColorStateList.valueOf(
                    ContextCompat.getColor(this, STATUS_TINTS[statusIndex])));
            snackbar("Session status · " + STATUS[statusIndex]);
        });

        presetButton.setOnClickListener(this::showPresetMenu);
        findViewById(R.id.btn_new_chat).setOnClickListener(v -> {
            if (chatFragment != null) {
                chatFragment.resetConversation();
            }
            sessionButton.setText("New chat");
            contextPercent = 4;
            updateContextIndicator(true);
            snackbar("New chat · context reset");
        });
        findViewById(R.id.btn_menu).setOnClickListener(this::showSessionMenu);

        modelChevron.setTag(modelChevronId);
    }

    private void showFolderMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor, Gravity.END);
        for (int i = 0; i < FOLDERS.length; i++) {
            popup.getMenu().add(0, i, i, FOLDERS[i]);
        }
        popup.setOnMenuItemClickListener(item -> {
            folderLabel.setText(FOLDERS[item.getItemId() % FOLDERS.length]);
            snackbar("Session folder · " + FOLDERS[item.getItemId() % FOLDERS.length]);
            return true;
        });
        popup.show();
    }

    private void showPresetMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor, Gravity.END);
        popup.getMenu().setGroupCheckable(0, true, true);
        for (int i = 0; i < PRESETS.length; i++) {
            popup.getMenu().add(0, i, i, PRESETS[i])
                    .setCheckable(true)
                    .setChecked(i == presetIndex);
        }
        popup.setOnMenuItemClickListener(item -> {
            presetIndex = item.getItemId();
            int tint = presetIndex == 1 ? R.color.neko_tertiary
                    : presetIndex == 2 ? R.color.neko_primary : R.color.neko_on_surface_variant;
            presetButton.setIconTint(ColorStateList.valueOf(ContextCompat.getColor(this, tint)));
            snackbar("Preset · " + PRESETS[presetIndex]);
            return true;
        });
        popup.show();
    }

    private void showSessionMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor, Gravity.END);
        popup.getMenu().add(0, 0, 0, "Rename session");
        popup.getMenu().add(0, 1, 1, "Export transcript");
        popup.getMenu().add(0, 2, 2, "Compress history");
        popup.getMenu().add(0, 3, 3, "Session settings");
        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 2:
                    new AttachmentsSheet(this).show();
                    break;
                case 3:
                    snackbar("Session settings · demo");
                    break;
                default:
                    snackbar(item.getTitle() + " · demo");
                    break;
            }
            return true;
        });
        popup.show();
    }

    private void setupInput() {
        inputLayout.setEndIconVisible(false);
        inputLayout.setEndIconOnClickListener(v -> openFullscreenInput());
        inputLayout.setEndIconActivated(true);

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
            snackbar("Quick message inserted");
        });
        modelButton.setOnClickListener(v -> openReasoning(false));
        modelChevron.setOnClickListener(v -> openReasoning(true));
        interpolationButton.setOnClickListener(v -> {
            interpolationIndex = (interpolationIndex + 1) % INTERPOLATIONS.length;
            interpolationButton.setIconTint(ColorStateList.valueOf(ContextCompat.getColor(this,
                    interpolationIndex == 2 ? R.color.neko_tertiary : R.color.neko_on_surface_variant)));
            snackbar("Interpolation · " + INTERPOLATIONS[interpolationIndex]);
        });
        interpolationButton.setTooltipText("Interpolation");
        recordButton.setTooltipText("Record");
        sendButton.setTooltipText("Send");
    }

    private void updateInputState() {
        boolean hasText = input.getText() != null
                && input.getText().toString().trim().length() > 0;
        inputLayout.setEndIconVisible(hasText);
        sendButton.setEnabled(generating || hasText);
        if (hasText) {
            inputLayout.setEndIconActivated(true);
        }
    }

    private void openReasoning(boolean models) {
        new ReasoningSheet(this, models, modelName, modelMeta, effortIndex, reasoningListener).show();
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
            snackbar("Write something first");
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
        snackbar("Generation stopped");
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
            snackbar("Neko answered · context " + contextPercent + "%");
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
        int targetBg = ContextCompat.getColor(this,
                on ? R.color.neko_tertiary_container : R.color.neko_primary_container);
        int targetIcon = ContextCompat.getColor(this,
                on ? R.color.neko_on_tertiary_container : R.color.neko_on_primary_container);

        ValueAnimator animator = ValueAnimator.ofArgb(sendBackgroundColor, targetBg);
        animator.setDuration(320);
        animator.addUpdateListener(a -> {
            sendBackgroundColor = ((Number) a.getAnimatedValue()).intValue();
            sendButton.setBackgroundTintList(ColorStateList.valueOf(sendBackgroundColor));
        });
        animator.start();

        sendButton.setIconResource(on ? R.drawable.ic_stop : R.drawable.ic_send);
        sendButton.setIconTint(ColorStateList.valueOf(targetIcon));
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
            contextLabel.setTextColor(ContextCompat.getColor(this, R.color.neko_error));
        } else if (generating) {
            contextLabel.setText("writing…");
            contextLabel.setTextColor(ContextCompat.getColor(this, R.color.neko_tertiary));
        } else {
            contextLabel.setText(contextPercent + "%");
            contextLabel.setTextColor(ContextCompat.getColor(this, R.color.neko_on_surface_variant));
        }
    }

    private void updateModelChip() {
        if (modelButton == null) {
            return;
        }
        modelButton.setIconResource(ReasoningSheet.iconForEffort(effortIndex));
        int tint;
        if (effortIndex <= 1) {
            tint = R.color.neko_on_primary_container;
        } else if (effortIndex >= 7) {
            tint = R.color.neko_on_tertiary_container;
        } else {
            tint = R.color.neko_on_primary_container;
        }
        modelButton.setIconTint(ColorStateList.valueOf(ContextCompat.getColor(this, tint)));
    }

    private void setRecording(boolean value) {
        recording = value;
        handler.removeCallbacks(recordingRunnable);
        if (value) {
            recordSeconds = 0;
            handler.post(recordingRunnable);
            snackbar("Recording · tap again to stop");
        } else {
            snackbar("Voice note discarded · demo");
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

    private void snackbar(String message) {
        Snackbar.make(root, message, Snackbar.LENGTH_SHORT).show();
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
