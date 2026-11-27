package dev.neko.chatdemo.sheet;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;

import java.util.ArrayList;
import java.util.List;

import dev.neko.chatdemo.R;

/**
 * Page two of the reasoning sheet: provider rail on the left, rich model cards on the right.
 * Hosted as a fragment so the sheet can swap pages with a shared axis X transition.
 */
public class ReasoningModelsFragment extends Fragment {

    private static final String ARG_PROVIDER = "provider";
    private static final String ARG_GROUP = "group";
    private static final String ARG_MODEL = "model";

    private final List<ReasoningData.Provider> providers = ReasoningData.build();
    private final boolean[] expanded = new boolean[8];
    private final ProviderAdapter providerAdapter = new ProviderAdapter();
    private final ModelAdapter modelAdapter = new ModelAdapter();

    private int selectedProvider;
    @Nullable
    private String selectedGroup;
    private String selectedModelName = "GPT-5.4";

    static ReasoningModelsFragment newInstance(int provider, @Nullable String group,
                                               @NonNull String modelName) {
        Bundle args = new Bundle();
        args.putInt(ARG_PROVIDER, provider);
        args.putString(ARG_GROUP, group);
        args.putString(ARG_MODEL, modelName);
        ReasoningModelsFragment fragment = new ReasoningModelsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_reasoning_models, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Bundle args = requireArguments();
        selectedProvider = Math.max(0, Math.min(providers.size() - 1, args.getInt(ARG_PROVIDER, 0)));
        selectedGroup = args.getString(ARG_GROUP);
        selectedModelName = args.getString(ARG_MODEL, "GPT-5.4");
        expanded[selectedProvider] = true;

        RecyclerView providerList = view.findViewById(R.id.provider_list);
        RecyclerView modelList = view.findViewById(R.id.model_list);
        providerList.setLayoutManager(new LinearLayoutManager(requireContext()));
        providerList.setAdapter(providerAdapter);
        modelList.setLayoutManager(new LinearLayoutManager(requireContext()));
        modelList.setAdapter(modelAdapter);

        providerAdapter.rebuild();
        updateModels();
    }

    private void updateModels() {
        List<ReasoningData.Model> models = new ArrayList<>();
        ReasoningData.Provider provider = providers.get(selectedProvider);
        if (selectedGroup == null) {
            for (ReasoningData.Group group : provider.groups) {
                java.util.Collections.addAll(models, group.models);
            }
        } else {
            for (ReasoningData.Group group : provider.groups) {
                if (group.name.equals(selectedGroup)) {
                    java.util.Collections.addAll(models, group.models);
                }
            }
        }
        modelAdapter.submit(models);

        if (getParentFragment() instanceof ReasoningSheet.PageHost) {
            ((ReasoningSheet.PageHost) getParentFragment())
                    .onModelsChanged(selectedProvider, selectedGroup, provider.name, models.size());
        }
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

    private void onGroupClick(int providerIndex, ReasoningData.Group group) {
        selectedProvider = providerIndex;
        selectedGroup = group.name;
        providerAdapter.rebuild();
        updateModels();
    }

    private void selectModel(ReasoningData.Model model) {
        selectedModelName = model.name;
        modelAdapter.notifyDataSetChanged();
        if (getParentFragment() instanceof ReasoningSheet.PageHost) {
            ((ReasoningSheet.PageHost) getParentFragment()).onModelPicked(model.name, model.meta);
        }
    }

    private int dp(float value) {
        return Math.round(value * requireContext().getResources().getDisplayMetrics().density);
    }

    // ---------------------------------------------------------------------------------------
    // Adapters

    private class ProviderAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private final List<Row> rows = new ArrayList<>();

        void rebuild() {
            rows.clear();
            for (int i = 0; i < providers.size(); i++) {
                ReasoningData.Provider provider = providers.get(i);
                rows.add(Row.provider(provider, i));
                if (expanded[i]) {
                    for (ReasoningData.Group group : provider.groups) {
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
                        ? androidx.appcompat.R.attr.colorPrimary
                        : com.google.android.material.R.attr.colorOnSurface));
                h.name.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
                h.chevron.setRotation(expanded[row.index] ? 180f : 0f);
                h.itemView.setOnClickListener(v -> onProviderClick(row.index));
            } else {
                GroupHolder h = (GroupHolder) holder;
                boolean selected = row.index == selectedProvider && row.group.name.equals(selectedGroup);
                h.name.setText(row.group.name);
                h.name.setTextColor(MaterialColors.getColor(h.itemView, selected
                        ? androidx.appcompat.R.attr.colorPrimary
                        : com.google.android.material.R.attr.colorOnSurfaceVariant));
                h.name.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
                int selectedBg = MaterialColors.getColor(h.itemView,
                        com.google.android.material.R.attr.colorSecondaryContainer);
                int rippleColor = MaterialColors.getColor(h.itemView,
                        androidx.appcompat.R.attr.colorPrimary);
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

        private final List<ReasoningData.Model> items = new ArrayList<>();

        void submit(List<ReasoningData.Model> models) {
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
            ReasoningData.Model model = items.get(position);
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
                    androidx.appcompat.R.attr.colorPrimary);
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

    private static class Row {
        final ReasoningData.Provider provider;
        final ReasoningData.Group group;
        final int index;

        private Row(ReasoningData.Provider provider, ReasoningData.Group group, int index) {
            this.provider = provider;
            this.group = group;
            this.index = index;
        }

        static Row provider(ReasoningData.Provider provider, int index) {
            return new Row(provider, null, index);
        }

        static Row group(ReasoningData.Provider provider, ReasoningData.Group group, int index) {
            return new Row(provider, group, index);
        }
    }
}
