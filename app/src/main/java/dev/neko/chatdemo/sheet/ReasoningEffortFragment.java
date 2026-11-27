package dev.neko.chatdemo.sheet;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.slider.Slider;

import dev.neko.chatdemo.R;

/**
 * Page one of the reasoning sheet: the current model row, the effort slider and the budget line.
 * Hosted as a fragment so the sheet can swap pages with a shared axis X transition.
 */
public class ReasoningEffortFragment extends Fragment {

    private static final String ARG_MODEL_NAME = "model_name";
    private static final String ARG_MODEL_META = "model_meta";
    private static final String ARG_EFFORT = "effort";
    private static final String ARG_BUDGET = "budget";

    private Slider slider;
    private LinearLayout labels;
    private TextView effortValue;
    private TextView budgetValue;

    static ReasoningEffortFragment newInstance(@NonNull String modelName, @NonNull String modelMeta,
                                               int effort, @NonNull String budgetLabel) {
        Bundle args = new Bundle();
        args.putString(ARG_MODEL_NAME, modelName);
        args.putString(ARG_MODEL_META, modelMeta);
        args.putInt(ARG_EFFORT, effort);
        args.putString(ARG_BUDGET, budgetLabel);
        ReasoningEffortFragment fragment = new ReasoningEffortFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_reasoning_effort, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Bundle args = requireArguments();
        TextView modelName = view.findViewById(R.id.current_model_name);
        TextView modelMeta = view.findViewById(R.id.current_model_meta);
        modelName.setText(args.getString(ARG_MODEL_NAME, "GPT-5.4"));
        modelMeta.setText(args.getString(ARG_MODEL_META, ""));

        effortValue = view.findViewById(R.id.effort_value);
        budgetValue = view.findViewById(R.id.budget_value);
        labels = view.findViewById(R.id.effort_labels);
        slider = view.findViewById(R.id.effort_slider);

        budgetValue.setText(args.getString(ARG_BUDGET, "not set · derived from effort"));

        view.findViewById(R.id.reasoning_model_row).setOnClickListener(v -> {
            if (getParentFragment() instanceof ReasoningSheet.PageHost) {
                ((ReasoningSheet.PageHost) getParentFragment()).onOpenModels();
            }
        });
        view.findViewById(R.id.effort_custom).setOnClickListener(v -> {
            if (getParentFragment() instanceof ReasoningSheet.PageHost) {
                ((ReasoningSheet.PageHost) getParentFragment()).onOpenCustomEffort();
            }
        });

        int effort = ReasoningSheet.clampEffort(args.getInt(ARG_EFFORT, 5));
        slider.setValue(effort);
        slider.addOnChangeListener((s, value, fromUser) -> {
            int index = ReasoningSheet.clampEffort(Math.round(value));
            applyLabels(index);
            if (fromUser) {
                budgetValue.setText("not set · derived from effort");
                if (getParentFragment() instanceof ReasoningSheet.PageHost) {
                    ((ReasoningSheet.PageHost) getParentFragment()).onEffortChanged(index, true);
                }
            }
        });
        applyLabels(effort);
    }

    /** Pushes a value that came from the custom effort dialog back into the page. */
    void setEffort(int index, @Nullable String budgetLabel) {
        if (slider == null) {
            return;
        }
        int clamped = ReasoningSheet.clampEffort(index);
        if (Math.round(slider.getValue()) != clamped) {
            slider.setValue(clamped);
        }
        applyLabels(clamped);
        if (budgetLabel != null && budgetValue != null) {
            budgetValue.setText(budgetLabel);
        }
    }

    private void applyLabels(int index) {
        if (effortValue == null || labels == null) {
            return;
        }
        effortValue.setText(ReasoningSheet.EFFORT_NAMES[index]);
        for (int i = 0; i < labels.getChildCount(); i++) {
            View child = labels.getChildAt(i);
            if (child instanceof TextView) {
                TextView label = (TextView) child;
                boolean selected = i == index;
                int color = MaterialColors.getColor(label,
                        selected
                                ? androidx.appcompat.R.attr.colorPrimary
                                : com.google.android.material.R.attr.colorOnSurfaceVariant);
                label.setTextColor(color);
                label.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
            }
        }
    }
}
