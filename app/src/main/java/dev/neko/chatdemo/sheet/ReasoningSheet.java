package dev.neko.chatdemo.sheet;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.transition.MaterialSharedAxis;

import java.util.Locale;

import dev.neko.chatdemo.R;

/**
 * Model + reasoning effort sheet.
 *
 * <p>The two pages live in child fragments and are swapped with a shared axis X transition. The
 * container keeps a fixed height, so the sheet never resizes in the middle of a page change (that
 * resize was what made the old view-based swap flicker).
 */
public class ReasoningSheet extends BottomSheetDialogFragment implements ReasoningPageHost {

    public interface Listener {
        void onModelSelected(@NonNull String name, @NonNull String meta);

        void onEffortSelected(int index, @Nullable String budgetLabel);
    }

    public static final String[] EFFORT_NAMES = {
            "Off", "Auto", "Minimal", "Low", "Medium", "High", "XHigh", "Max", "Ultra"
    };

    private static final int[] EFFORT_ICONS = {
            R.drawable.ic_power_settings_new,
            R.drawable.ic_auto_awesome,
            R.drawable.ic_bolt,
            R.drawable.ic_bolt,
            R.drawable.ic_psychology,
            R.drawable.ic_psychology,
            R.drawable.ic_rocket_launch,
            R.drawable.ic_rocket_launch,
            R.drawable.ic_diamond
    };

    private static final String ARG_OPEN_MODELS = "open_models";
    private static final String ARG_MODEL_NAME = "model_name";
    private static final String ARG_MODEL_META = "model_meta";
    private static final String ARG_EFFORT = "effort";

    public static int iconForEffort(int index) {
        int i = Math.max(0, Math.min(EFFORT_ICONS.length - 1, index));
        return EFFORT_ICONS[i];
    }

    static int clampEffort(int index) {
        return Math.max(0, Math.min(EFFORT_NAMES.length - 1, index));
    }

    public static ReasoningSheet newInstance(boolean openModels, @NonNull String modelName,
                                             @NonNull String modelMeta, int effort) {
        ReasoningSheet sheet = new ReasoningSheet();
        Bundle args = new Bundle();
        args.putBoolean(ARG_OPEN_MODELS, openModels);
        args.putString(ARG_MODEL_NAME, modelName);
        args.putString(ARG_MODEL_META, modelMeta);
        args.putInt(ARG_EFFORT, effort);
        sheet.setArguments(args);
        return sheet;
    }

    private TextView title;
    private TextView subtitle;
    private MaterialButton backButton;

    private boolean openModelsAtStart;
    private boolean onModels;
    private int effortIndex = 5;
    private int budget = -1;
    private int selectedProvider;
    @Nullable
    private String selectedGroup;
    private String modelsSubtitle = "";
    private String modelName = "GPT-5.4";
    private String modelMeta = "";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = requireArguments();
        openModelsAtStart = args.getBoolean(ARG_OPEN_MODELS, false);
        modelName = args.getString(ARG_MODEL_NAME, "GPT-5.4");
        modelMeta = args.getString(ARG_MODEL_META, "");
        effortIndex = clampEffort(args.getInt(ARG_EFFORT, 5));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sheet_reasoning, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        title = view.findViewById(R.id.reasoning_title);
        subtitle = view.findViewById(R.id.reasoning_subtitle);
        backButton = view.findViewById(R.id.reasoning_back);
        MaterialButton close = view.findViewById(R.id.reasoning_close);
        close.setOnClickListener(v -> dismiss());
        backButton.setOnClickListener(v -> showEffort(true));

        // Both pages scroll inside this box, so the sheet keeps one height for the whole session.
        View pages = view.findViewById(R.id.reasoning_pages);
        int wanted = Math.round(460 * getResources().getDisplayMetrics().density);
        int limit = Math.round(getResources().getDisplayMetrics().heightPixels * 0.66f);
        ViewGroup.LayoutParams params = pages.getLayoutParams();
        params.height = Math.min(wanted, limit);
        pages.setLayoutParams(params);

        if (savedInstanceState == null) {
            if (openModelsAtStart) {
                showModels(false);
            } else {
                showEffort(false);
            }
        } else {
            onModels = getChildFragmentManager().findFragmentById(R.id.reasoning_pages)
                    instanceof ReasoningModelsFragment;
            updateHeader();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog instanceof BottomSheetDialog) {
            BottomSheetDialog sheet = (BottomSheetDialog) dialog;
            sheet.getBehavior().setSkipCollapsed(true);
            sheet.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
        }
        if (dialog != null) {
            dialog.setOnKeyListener((d, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_BACK
                        && event.getAction() == KeyEvent.ACTION_UP
                        && onModels) {
                    showEffort(true);
                    return true;
                }
                return false;
            });
        }
    }

    // ---------------------------------------------------------------------------------------
    // Page navigation

    private void showModels(boolean animate) {
        onModels = true;
        updateHeader();
        navigate(ReasoningModelsFragment.newInstance(selectedProvider, selectedGroup, modelName),
                true, animate);
    }

    private void showEffort(boolean animate) {
        onModels = false;
        updateHeader();
        navigate(ReasoningEffortFragment.newInstance(modelName, modelMeta, effortIndex,
                budgetLabelText()), false, animate);
    }

    private void navigate(Fragment page, boolean forward, boolean animate) {
        Fragment current = getChildFragmentManager().findFragmentById(R.id.reasoning_pages);
        FragmentTransaction transaction = getChildFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true);
        if (animate) {
            MaterialSharedAxis enter = new MaterialSharedAxis(MaterialSharedAxis.X, forward);
            MaterialSharedAxis exit = new MaterialSharedAxis(MaterialSharedAxis.X, forward);
            enter.setDuration(320);
            exit.setDuration(320);
            page.setEnterTransition(enter);
            page.setExitTransition(exit);
            if (current != null) {
                current.setExitTransition(exit);
                current.setReenterTransition(enter);
            }
        }
        transaction.replace(R.id.reasoning_pages, page).commit();
    }

    private void updateHeader() {
        if (title == null || subtitle == null || backButton == null) {
            return;
        }
        if (onModels) {
            title.setText(R.string.reasoning_models_title);
            subtitle.setText(modelsSubtitle);
            backButton.setVisibility(View.VISIBLE);
        } else {
            title.setText(R.string.reasoning_page_title);
            subtitle.setText(R.string.reasoning_page_subtitle);
            backButton.setVisibility(View.INVISIBLE);
        }
    }

    // ---------------------------------------------------------------------------------------
    // Page callbacks

    @Override
    public void onOpenModels() {
        showModels(true);
    }

    @Override
    public void onEffortChanged(int index, boolean fromUser) {
        effortIndex = clampEffort(index);
        if (fromUser) {
            budget = -1;
        }
        notifyEffort();
    }

    @Override
    public void onOpenCustomEffort() {
        openCustomDialog();
    }

    @Override
    public void onModelPicked(@NonNull String name, @NonNull String meta) {
        modelName = name;
        modelMeta = meta;
        Listener listener = listener();
        if (listener != null) {
            listener.onModelSelected(name, meta);
        }
        showEffort(true);
    }

    @Override
    public void onModelsChanged(int providerIndex, @Nullable String group,
                                @NonNull String providerName, int count) {
        selectedProvider = providerIndex;
        selectedGroup = group;
        modelsSubtitle = getString(R.string.reasoning_models_count, providerName, count);
        if (onModels && subtitle != null) {
            subtitle.setText(modelsSubtitle);
        }
    }

    // ---------------------------------------------------------------------------------------
    // Effort

    private void openCustomDialog() {
        View content = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_reasoning_custom, null);
        MaterialRadioButton effortMode = content.findViewById(R.id.param_effort);
        MaterialRadioButton budgetMode = content.findViewById(R.id.param_budget);
        TextInputLayout valueLayout = content.findViewById(R.id.custom_value_layout);
        TextInputEditText value = content.findViewById(R.id.custom_value);

        value.setText(EFFORT_NAMES[effortIndex].toLowerCase(Locale.US));

        View.OnClickListener modeListener = v -> {
            boolean isBudget = budgetMode.isChecked();
            valueLayout.setHelperText(isBudget
                    ? R.string.reasoning_custom_helper_budget
                    : R.string.reasoning_custom_helper_effort);
            value.setText(isBudget
                    ? String.valueOf(budget > 0 ? budget : 12288)
                    : EFFORT_NAMES[effortIndex].toLowerCase(Locale.US));
            valueLayout.setError(null);
        };
        effortMode.setOnClickListener(modeListener);
        budgetMode.setOnClickListener(modeListener);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setView(content)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_apply, null);
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String raw = value.getText() == null ? "" : value.getText().toString().trim();
                    if (budgetMode.isChecked()) {
                        int tokens;
                        try {
                            tokens = Integer.parseInt(raw.replace(",", "").replace("_", ""));
                        } catch (NumberFormatException e) {
                            valueLayout.setError(getString(R.string.reasoning_custom_number));
                            return;
                        }
                        if (tokens < 0 || tokens > 32768) {
                            valueLayout.setError(getString(R.string.reasoning_custom_range));
                            return;
                        }
                        budget = tokens;
                        syncEffortPage(effortIndex, budgetLabel());
                        notifyEffort();
                        dialog.dismiss();
                        return;
                    }

                    int index = parseEffort(raw);
                    if (index < 0) {
                        valueLayout.setError(getString(R.string.reasoning_custom_unknown));
                        return;
                    }
                    effortIndex = index;
                    budget = -1;
                    syncEffortPage(index, getString(R.string.reasoning_budget_unset));
                    notifyEffort();
                    valueLayout.setError(null);
                    dialog.dismiss();
                }));
        dialog.show();
    }

    private void syncEffortPage(int index, @Nullable String budgetLabel) {
        Fragment page = getChildFragmentManager().findFragmentById(R.id.reasoning_pages);
        if (page instanceof ReasoningEffortFragment) {
            ((ReasoningEffortFragment) page).setEffort(index, budgetLabel);
        }
    }

    private void notifyEffort() {
        Listener listener = listener();
        if (listener != null) {
            listener.onEffortSelected(effortIndex, budget > 0 ? budgetLabel() : null);
        }
    }

    @Nullable
    private Listener listener() {
        Activity activity = getActivity();
        return activity instanceof Listener ? (Listener) activity : null;
    }

    private String budgetLabel() {
        return getString(R.string.reasoning_budget_custom,
                String.format(Locale.US, "%,d", budget));
    }

    private String budgetLabelText() {
        return budget > 0 ? budgetLabel() : getString(R.string.reasoning_budget_unset);
    }

    private int parseEffort(String raw) {
        String key = raw.toLowerCase(Locale.US).replace("-", "").replace("_", "").replace(" ", "");
        if (key.equals("min")) {
            key = "minimal";
        }
        if (key.equals("med")) {
            key = "medium";
        }
        for (int i = 0; i < EFFORT_NAMES.length; i++) {
            if (EFFORT_NAMES[i].toLowerCase(Locale.US).equals(key)) {
                return i;
            }
        }
        return -1;
    }
}
