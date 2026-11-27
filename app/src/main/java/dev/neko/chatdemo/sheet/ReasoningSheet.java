package dev.neko.chatdemo.sheet;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.TransitionManager;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.transition.MaterialSharedAxis;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import dev.neko.chatdemo.R;

/**
 * Model + reasoning effort sheet. The model list is a two column layout: providers on the narrow
 * left rail (expandable) and rich model cards on the right.
 */
public class ReasoningSheet extends BottomSheetDialog {

    public interface Listener {
        void onModelSelected(@NonNull String name, @NonNull String meta);

        void onEffortSelected(int index, @Nullable String budgetLabel);
    }

    public static final String[] EFFORT_NAMES = {
            "Off", "Auto", "Minimal", "Low", "Medium", "High", "XHigh", "Max", "Ultra"
    };

    private static final String[] EFFORT_LABELS = {
            "Off", "Auto", "Min", "Low", "Med", "High", "XHigh", "Max", "Ultra"
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

    public static int iconForEffort(int index) {
        int i = Math.max(0, Math.min(EFFORT_ICONS.length - 1, index));
        return EFFORT_ICONS[i];
    }

    private final Listener listener;
    private final boolean openModelsAtStart;

    private final List<Provider> providers = new ArrayList<>();
    private final boolean[] expanded = new boolean[8];

    private final View mainPage;
    private final View modelsPage;
    private final RecyclerView providerList;
    private final RecyclerView modelList;
    private final TextView title;
    private final TextView subtitle;
    private final TextView currentModelName;
    private final TextView currentModelMeta;
    private final TextView effortValue;
    private final TextView budgetValue;
    private final Slider effortSlider;
    private final LinearLayout effortLabels;
    private final MaterialButton backButton;

    private final ProviderAdapter providerAdapter = new ProviderAdapter();
    private final ModelAdapter modelAdapter = new ModelAdapter();

    private int effortIndex = 5;
    private int budget = -1;
    private int selectedProvider = 0;
    private String selectedGroup;
    private String selectedModelName = "GPT-5.4";
    private String selectedModelMeta = "OpenAI · 200k context · vision";
    private boolean onModels;

    public ReasoningSheet(@NonNull Context context, boolean openModels, @NonNull String modelName,
                          @NonNull String modelMeta, int currentEffort, @NonNull Listener listener) {
        super(context);
        this.listener = listener;
        this.openModelsAtStart = openModels;
        this.selectedModelName = modelName;
        this.selectedModelMeta = modelMeta;
        this.effortIndex = Math.max(0, Math.min(EFFORT_NAMES.length - 1, currentEffort));

        View content = LayoutInflater.from(context).inflate(R.layout.sheet_reasoning, null);
        setContentView(content);

        mainPage = content.findViewById(R.id.reasoning_main_page);
        modelsPage = content.findViewById(R.id.reasoning_models_page);
        providerList = content.findViewById(R.id.provider_list);
        modelList = content.findViewById(R.id.model_list);
        title = content.findViewById(R.id.reasoning_title);
        subtitle = content.findViewById(R.id.reasoning_subtitle);
        currentModelName = content.findViewById(R.id.current_model_name);
        currentModelMeta = content.findViewById(R.id.current_model_meta);
        effortValue = content.findViewById(R.id.effort_value);
        budgetValue = content.findViewById(R.id.budget_value);
        effortSlider = content.findViewById(R.id.effort_slider);
        effortLabels = content.findViewById(R.id.effort_labels);
        backButton = content.findViewById(R.id.reasoning_back);
        MaterialButton close = content.findViewById(R.id.reasoning_close);

        buildData();
        providerList.setLayoutManager(new LinearLayoutManager(context));
        providerList.setAdapter(providerAdapter);
        modelList.setLayoutManager(new LinearLayoutManager(context));
        modelList.setAdapter(modelAdapter);
        providerAdapter.rebuild();
        updateModels();

        currentModelName.setText(selectedModelName);
        currentModelMeta.setText(selectedModelMeta);

        effortSlider.setValue(effortIndex);
        effortSlider.addOnChangeListener((slider, value, fromUser) ->
                applyEffort(Math.round(value), fromUser));

        content.findViewById(R.id.reasoning_model_row).setOnClickListener(v -> showModels(true));
        close.setOnClickListener(v -> dismiss());
        backButton.setOnClickListener(v -> showMain());
        content.findViewById(R.id.effort_custom).setOnClickListener(v -> openCustomDialog());

        applyEffort(effortIndex, false);

        setOnShowListener(dialog -> {
            BottomSheetBehavior<FrameLayout> behavior = getBehavior();
            behavior.setSkipCollapsed(true);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            if (openModelsAtStart) {
                showModels(false);
            }
        });
    }

    // ---------------------------------------------------------------------------------------
    // Effort

    private void applyEffort(int index, boolean fromUser) {
        effortIndex = Math.max(0, Math.min(EFFORT_NAMES.length - 1, index));
        effortValue.setText(EFFORT_NAMES[effortIndex]);

        for (int i = 0; i < effortLabels.getChildCount(); i++) {
            View child = effortLabels.getChildAt(i);
            if (child instanceof TextView) {
                TextView label = (TextView) child;
                boolean selected = i == effortIndex;
                int color = MaterialColors.getColor(label,
                        selected
                                ? com.google.android.material.R.attr.colorPrimary
                                : com.google.android.material.R.attr.colorOnSurfaceVariant);
                label.setTextColor(color);
                label.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
            }
        }

        if (fromUser) {
            budget = -1;
            budgetValue.setText("not set · derived from effort");
        }

        listener.onEffortSelected(effortIndex, budget > 0 ? budgetLabel() : null);
    }

    private String budgetLabel() {
        return String.format(Locale.US, "%,d tokens · custom", budget);
    }

    private void openCustomDialog() {
        View content = LayoutInflater.from(getContext()).inflate(R.layout.dialog_reasoning_custom, null);
        MaterialRadioButton effortMode = content.findViewById(R.id.param_effort);
        MaterialRadioButton budgetMode = content.findViewById(R.id.param_budget);
        TextInputLayout valueLayout = content.findViewById(R.id.custom_value_layout);
        TextInputEditText value = content.findViewById(R.id.custom_value);

        value.setText(EFFORT_NAMES[effortIndex].toLowerCase(Locale.US));

        View.OnClickListener modeListener = v -> {
            boolean isBudget = budgetMode.isChecked();
            valueLayout.setHelperText(isBudget
                    ? "0 – 32,768 tokens"
                    : "off · auto · minimal · low · medium · high · xhigh · max · ultra");
            value.setText(isBudget
                    ? String.valueOf(budget > 0 ? budget : 12288)
                    : EFFORT_NAMES[effortIndex].toLowerCase(Locale.US));
            valueLayout.setError(null);
        };
        effortMode.setOnClickListener(modeListener);
        budgetMode.setOnClickListener(modeListener);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext())
                .setView(content)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Apply", null);
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String raw = value.getText() == null ? "" : value.getText().toString().trim();
                    if (budgetMode.isChecked()) {
                        int tokens;
                        try {
                            tokens = Integer.parseInt(raw.replace(",", "").replace("_", ""));
                        } catch (NumberFormatException e) {
                            valueLayout.setError("Enter a number");
                            return;
                        }
                        if (tokens < 0 || tokens > 32768) {
                            valueLayout.setError("0 – 32,768");
                            return;
                        }
                        budget = tokens;
                        budgetValue.setText(budgetLabel());
                        listener.onEffortSelected(effortIndex, budgetLabel());
                        dialog.dismiss();
                        return;
                    }

                    int index = parseEffort(raw);
                    if (index < 0) {
                        valueLayout.setError("Unknown effort");
                        return;
                    }
                    effortSlider.setValue(index);
                    valueLayout.setError(null);
                    dialog.dismiss();
                }));
        dialog.show();
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

    // ---------------------------------------------------------------------------------------
    // Pages

    private void showModels(boolean forward) {
        onModels = true;
        MaterialSharedAxis axis = new MaterialSharedAxis(MaterialSharedAxis.X, forward);
        axis.setDuration(300);
        TransitionManager.beginDelayedTransition((ViewGroup) mainPage.getParent(), axis);
        mainPage.setVisibility(View.GONE);
        modelsPage.setVisibility(View.VISIBLE);
        backButton.setVisibility(View.VISIBLE);
        title.setText("Model selection");
        Provider provider = providers.get(selectedProvider);
        subtitle.setText(provider.name + " · " + modelAdapter.getItemCount() + " models");
    }

    private void showMain() {
        if (!onModels) {
            return;
        }
        onModels = false;
        MaterialSharedAxis axis = new MaterialSharedAxis(MaterialSharedAxis.X, false);
        axis.setDuration(300);
        TransitionManager.beginDelayedTransition((ViewGroup) mainPage.getParent(), axis);
        modelsPage.setVisibility(View.GONE);
        mainPage.setVisibility(View.VISIBLE);
        backButton.setVisibility(View.INVISIBLE);
        title.setText("Reasoning & model");
        subtitle.setText("Effort, budget and the model behind the turn");
        providerAdapter.rebuild();
    }

    @Override
    public void onBackPressed() {
        if (onModels) {
            showMain();
        } else {
            super.onBackPressed();
        }
    }

    // ---------------------------------------------------------------------------------------
    // Data

    private void buildData() {
        Provider openAi = new Provider("OpenAI");
        openAi.groups.add(new Group("Flagship", new Model[]{
                new Model("GPT-5.4", "OpenAI · 200k context · vision",
                        "Flagship reasoning, tools and vision.", "G", 0),
                new Model("GPT-5.4 Pro", "OpenAI · 400k context · vision",
                        "Slow, expensive, very good at hard plans.", "P", 0)
        }));
        openAi.groups.add(new Group("Fast", new Model[]{
                new Model("GPT-5.4 mini", "OpenAI · 128k context",
                        "Cheap and quick for small refactors.", "m", 2)
        }));

        Provider anthropic = new Provider("Anthropic");
        anthropic.groups.add(new Group("Flagship", new Model[]{
                new Model("Claude Opus 4.5", "Anthropic · 200k context",
                        "Long reviews and careful edits.", "C", 1)
        }));
        anthropic.groups.add(new Group("Balanced", new Model[]{
                new Model("Claude Sonnet 4.5", "Anthropic · 200k context",
                        "The everyday pairing model.", "S", 1)
        }));

        Provider google = new Provider("Google");
        google.groups.add(new Group("Fast", new Model[]{
                new Model("Gemini 3 Flash", "Google · 1M context",
                        "Huge documents, fast first token.", "G", 2)
        }));

        Provider local = new Provider("Local");
        local.groups.add(new Group("On device", new Model[]{
                new Model("Neko-8B", "Local · 32k context",
                        "Private, runs on the phone.", "N", 3)
        }));

        providers.add(openAi);
        providers.add(anthropic);
        providers.add(google);
        providers.add(local);
        expanded[0] = true;
    }

    private void updateModels() {
        List<Model> models = new ArrayList<>();
        if (selectedProvider >= 0 && selectedProvider < providers.size()) {
            Provider provider = providers.get(selectedProvider);
            if (selectedGroup == null) {
                for (Group group : provider.groups) {
                    models.addAll(group.models);
                }
            } else {
                for (Group group : provider.groups) {
                    if (group.name.equals(selectedGroup)) {
                        models.addAll(group.models);
                    }
                }
            }
        }
        modelAdapter.submit(models);
        if (onModels) {
            Provider provider = providers.get(selectedProvider);
            subtitle.setText(provider.name + " · " + models.size() + " models");
        }
    }

    private void selectModel(Model model) {
        selectedModelName = model.name;
        selectedModelMeta = model.meta;
        currentModelName.setText(model.name);
        currentModelMeta.setText(model.meta);
        modelAdapter.notifyDataSetChanged();
        listener.onModelSelected(model.name, model.meta);
        showMain();
    }

    private void onProviderClick(int index) {
        if (selectedProvider == index && selectedGroup == null && expanded[index]) {
            expanded[index] = false;
        } else {
            expanded[index] = true;
            selectedProvider = index;
            selectedGroup = null;
        }
        providerAdapter.rebuild();
        updateModels();
    }

    private void onGroupClick(int providerIndex, Group group) {
        selectedProvider = providerIndex;
        selectedGroup = group.name;
        providerAdapter.rebuild();
        updateModels();
    }

    // ---------------------------------------------------------------------------------------
    // Adapters

    private class ProviderAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private final List<Row> rows = new ArrayList<>();

        void rebuild() {
            rows.clear();
            for (int i = 0; i < providers.size(); i++) {
                Provider provider = providers.get(i);
                rows.add(Row.provider(provider, i));
                if (expanded[i]) {
                    for (Group group : provider.groups) {
                        rows.add(Row.group(provider, group, i));
                    }
                }
            }
            notifyDataSetChanged();
        }

        @Override
        public int getItemViewType(int position) {
            return rows.get(position).group == null ? 0 : 1;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            if (viewType == 0) {
                return new ProviderHolder(inflater.inflate(R.layout.item_provider_group, parent, false));
            }
            return new GroupHolder(inflater.inflate(R.layout.item_provider_child, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Row row = rows.get(position);
            if (holder instanceof ProviderHolder) {
                ProviderHolder h = (ProviderHolder) holder;
                boolean selected = row.index == selectedProvider && selectedGroup == null;
                h.name.setText(row.provider.name);
                h.indicator.setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
                h.name.setTextColor(MaterialColors.getColor(h.itemView, selected
                        ? com.google.android.material.R.attr.colorPrimary
                        : com.google.android.material.R.attr.colorOnSurface));
                h.name.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
                h.chevron.setRotation(expanded[row.index] ? 180f : 0f);
                h.itemView.setOnClickListener(v -> onProviderClick(row.index));
            } else {
                GroupHolder h = (GroupHolder) holder;
                boolean selected = row.index == selectedProvider && row.group.name.equals(selectedGroup);
                h.name.setText(row.group.name);
                h.name.setTextColor(MaterialColors.getColor(h.itemView, selected
                        ? com.google.android.material.R.attr.colorPrimary
                        : com.google.android.material.R.attr.colorOnSurfaceVariant));
                h.name.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
                int selectedBg = MaterialColors.getColor(h.itemView,
                        com.google.android.material.R.attr.colorSecondaryContainer);
                int rippleColor = MaterialColors.getColor(h.itemView,
                        com.google.android.material.R.attr.colorPrimary);
                rippleColor = (rippleColor & 0x00FFFFFF) | 0x33000000;
                Drawable bg = new ColorDrawable(selected ? selectedBg : Color.TRANSPARENT);
                h.name.setBackground(new RippleDrawable(ColorStateList.valueOf(rippleColor), bg, null));
                h.itemView.setOnClickListener(v -> onGroupClick(row.index, row.group));
            }
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }

        class ProviderHolder extends RecyclerView.ViewHolder {
            final TextView name;
            final ImageView chevron;
            final View indicator;

            ProviderHolder(@NonNull View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.provider_name);
                chevron = itemView.findViewById(R.id.provider_chevron);
                indicator = itemView.findViewById(R.id.provider_indicator);
            }
        }

        class GroupHolder extends RecyclerView.ViewHolder {
            final TextView name;

            GroupHolder(@NonNull View itemView) {
                super(itemView);
                name = (TextView) itemView;
            }
        }
    }

    private class ModelAdapter extends RecyclerView.Adapter<ModelAdapter.ModelHolder> {

        private final List<Model> items = new ArrayList<>();

        void submit(List<Model> models) {
            items.clear();
            items.addAll(models);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ModelHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ModelHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_model, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ModelHolder holder, int position) {
            Model model = items.get(position);
            holder.name.setText(model.name);
            holder.meta.setText(model.meta);
            holder.desc.setText(model.desc);
            holder.avatar.setText(model.avatar);

            int bgAttr;
            int fgAttr;
            switch (model.style) {
                case 1:
                    bgAttr = com.google.android.material.R.attr.colorSecondaryContainer;
                    fgAttr = com.google.android.material.R.attr.colorOnSecondaryContainer;
                    break;
                case 2:
                    bgAttr = com.google.android.material.R.attr.colorTertiaryContainer;
                    fgAttr = com.google.android.material.R.attr.colorOnTertiaryContainer;
                    break;
                case 3:
                    bgAttr = com.google.android.material.R.attr.colorSurfaceContainerHighest;
                    fgAttr = com.google.android.material.R.attr.colorOnSurface;
                    break;
                case 0:
                default:
                    bgAttr = com.google.android.material.R.attr.colorPrimaryContainer;
                    fgAttr = com.google.android.material.R.attr.colorOnPrimaryContainer;
                    break;
            }
            holder.avatar.setBackgroundTintList(ColorStateList.valueOf(
                    MaterialColors.getColor(holder.itemView, bgAttr)));
            holder.avatar.setTextColor(MaterialColors.getColor(holder.itemView, fgAttr));

            boolean selected = model.name.equals(selectedModelName);
            holder.check.setVisibility(selected ? View.VISIBLE : View.GONE);
            int primary = MaterialColors.getColor(holder.itemView,
                    com.google.android.material.R.attr.colorPrimary);
            int outline = MaterialColors.getColor(holder.itemView,
                    com.google.android.material.R.attr.colorOutlineVariant);
            int low = MaterialColors.getColor(holder.itemView,
                    com.google.android.material.R.attr.colorSurfaceContainerLow);
            int high = MaterialColors.getColor(holder.itemView,
                    com.google.android.material.R.attr.colorSurfaceContainerHigh);

            holder.card.setStrokeColor(selected ? primary : outline);
            holder.card.setStrokeWidth(dp(selected ? 1.5f : 1f));
            holder.card.setCardBackgroundColor(selected ? high : low);
            holder.itemView.setOnClickListener(v -> selectModel(model));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ModelHolder extends RecyclerView.ViewHolder {
            final MaterialCardView card;
            final TextView avatar;
            final TextView name;
            final TextView meta;
            final TextView desc;
            final ImageView check;

            ModelHolder(@NonNull View itemView) {
                super(itemView);
                card = (MaterialCardView) itemView;
                avatar = itemView.findViewById(R.id.model_avatar);
                name = itemView.findViewById(R.id.model_name);
                meta = itemView.findViewById(R.id.model_meta);
                desc = itemView.findViewById(R.id.model_desc);
                check = itemView.findViewById(R.id.model_check);
            }
        }
    }

    private int dp(float value) {
        return Math.round(value * getContext().getResources().getDisplayMetrics().density);
    }

    // ---------------------------------------------------------------------------------------
    // Types

    private static class Provider {
        final String name;
        final List<Group> groups = new ArrayList<>();

        Provider(String name) {
            this.name = name;
        }
    }

    private static class Group {
        final String name;
        final Model[] models;

        Group(String name, Model[] models) {
            this.name = name;
            this.models = models;
        }
    }

    private static class Model {
        final String name;
        final String meta;
        final String desc;
        final String avatar;
        final int style;

        Model(String name, String meta, String desc, String avatar, int style) {
            this.name = name;
            this.meta = meta;
            this.desc = desc;
            this.avatar = avatar;
            this.style = style;
        }
    }

    private static class Row {
        final Provider provider;
        final Group group;
        final int index;

        private Row(Provider provider, Group group, int index) {
            this.provider = provider;
            this.group = group;
            this.index = index;
        }

        static Row provider(Provider provider, int index) {
            return new Row(provider, null, index);
        }

        static Row group(Provider provider, Group group, int index) {
            return new Row(provider, group, index);
        }
    }
}
