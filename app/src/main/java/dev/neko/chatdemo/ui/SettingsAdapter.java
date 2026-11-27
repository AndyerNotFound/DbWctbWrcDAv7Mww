package dev.neko.chatdemo.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.ChangeBounds;
import androidx.transition.TransitionManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import dev.neko.chatdemo.R;

/**
 * Multi-section settings list: colour mode group, preference rows and the expandable provider
 * block. Providers can be reordered by long pressing a card (see the ItemTouchHelper in
 * SettingsActivity).
 */
public class SettingsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface Callbacks {
        void onColorMode(int mode);

        void onPreference(@NonNull String key);

        void onImportProvider();

        void onAddProvider();

        void onFetchModels(@NonNull ProviderItem provider);
    }

    public static class ProviderItem {
        public final String name;
        public final String letter;
        public final String baseUrl;
        public final String apiKey;
        public final int models;
        public boolean expanded;

        public ProviderItem(String name, String letter, String baseUrl, String apiKey, int models) {
            this.name = name;
            this.letter = letter;
            this.baseUrl = baseUrl;
            this.apiKey = apiKey;
            this.models = models;
        }
    }

    private static final int TYPE_COLOR = 0;
    private static final int TYPE_SECTION = 1;
    private static final int TYPE_PREFERENCE = 2;
    private static final int TYPE_SEARCH = 3;
    private static final int TYPE_PROVIDER = 4;
    private static final int TYPE_EMPTY = 5;

    private final List<ProviderItem> providers;
    private final List<Row> rows = new ArrayList<>();
    private final Callbacks callbacks;
    private final Context context;

    private int colorMode;
    private String query = "";

    public SettingsAdapter(@NonNull Context context, @NonNull List<ProviderItem> providers,
                           int colorMode, @NonNull Callbacks callbacks) {
        this.context = context;
        this.providers = providers;
        this.colorMode = colorMode;
        this.callbacks = callbacks;
        rebuild(false);
    }

    // ---------------------------------------------------------------------------------------
    // Rows

    private static class Row {
        final int type;
        String title;
        String subtitle;
        String key;
        int icon;
        boolean actions;
        ProviderItem provider;

        Row(int type) {
            this.type = type;
        }
    }

    private Row section(int titleRes, boolean actions) {
        Row row = new Row(TYPE_SECTION);
        row.title = context.getString(titleRes);
        row.actions = actions;
        return row;
    }

    private Row preference(int titleRes, int subtitleRes, String key, int iconRes) {
        Row row = new Row(TYPE_PREFERENCE);
        row.title = context.getString(titleRes);
        row.subtitle = context.getString(subtitleRes);
        row.key = key;
        row.icon = iconRes;
        return row;
    }

    public void rebuild(boolean animate) {
        List<Row> next = new ArrayList<>();

        next.add(new Row(TYPE_COLOR));

        next.add(section(R.string.settings_preferences, false));
        next.add(preference(R.string.settings_theme, R.string.settings_theme_sub,
                "theme", R.drawable.ic_tune));
        next.add(preference(R.string.settings_general, R.string.settings_general_sub,
                "general", R.drawable.ic_settings));
        next.add(preference(R.string.settings_ui, R.string.settings_ui_sub,
                "ui", R.drawable.ic_animation));
        next.add(preference(R.string.settings_notifications, R.string.settings_notifications_sub,
                "notifications", R.drawable.ic_chat_bubble));
        next.add(preference(R.string.settings_network, R.string.settings_network_sub,
                "network", R.drawable.ic_cloud));

        next.add(section(R.string.settings_providers, true));
        next.add(new Row(TYPE_SEARCH));

        int shown = 0;
        for (ProviderItem provider : providers) {
            if (!matches(provider)) {
                continue;
            }
            Row row = new Row(TYPE_PROVIDER);
            row.provider = provider;
            next.add(row);
            shown++;
        }
        if (shown == 0) {
            next.add(new Row(TYPE_EMPTY));
        }

        if (animate && getItemCount() > 0) {
            DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new RowDiff(rows, next));
            rows.clear();
            rows.addAll(next);
            diff.dispatchUpdatesTo(this);
        } else {
            rows.clear();
            rows.addAll(next);
            notifyDataSetChanged();
        }
    }

    private boolean matches(ProviderItem provider) {
        return query.isEmpty()
                || provider.name.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT));
    }

    public void setQuery(String query) {
        this.query = query == null ? "" : query;
        rebuild(true);
    }

    public void setColorMode(int mode) {
        if (colorMode != mode) {
            colorMode = mode;
            notifyItemChanged(0);
        }
    }

    public boolean isProviderPosition(int position) {
        return position >= 0 && position < rows.size() && rows.get(position).type == TYPE_PROVIDER;
    }

    public boolean moveProvider(int from, int to) {
        if (!isProviderPosition(from) || !isProviderPosition(to)) {
            return false;
        }
        Collections.swap(rows, from, to);
        providers.clear();
        for (Row row : rows) {
            if (row.provider != null) {
                providers.add(row.provider);
            }
        }
        notifyItemMoved(from, to);
        return true;
    }

    // ---------------------------------------------------------------------------------------
    // Adapter plumbing

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case TYPE_COLOR:
                return new ColorHolder(inflater.inflate(R.layout.item_settings_color_mode, parent, false));
            case TYPE_SECTION:
                return new SectionHolder(inflater.inflate(R.layout.item_settings_section, parent, false));
            case TYPE_PREFERENCE:
                return new PreferenceHolder(inflater.inflate(R.layout.item_settings_preference, parent, false));
            case TYPE_SEARCH:
                return new SearchHolder(inflater.inflate(R.layout.item_settings_search, parent, false));
            case TYPE_EMPTY:
                return new EmptyHolder(inflater.inflate(R.layout.item_settings_preference, parent, false));
            case TYPE_PROVIDER:
            default:
                return new ProviderHolder(inflater.inflate(R.layout.item_settings_provider, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Row row = rows.get(position);
        if (holder instanceof ColorHolder) {
            ((ColorHolder) holder).bind(colorMode);
        } else if (holder instanceof SectionHolder) {
            ((SectionHolder) holder).bind(row);
        } else if (holder instanceof PreferenceHolder) {
            ((PreferenceHolder) holder).bind(row);
        } else if (holder instanceof ProviderHolder) {
            ((ProviderHolder) holder).bind(row.provider);
        } else if (holder instanceof EmptyHolder) {
            ((EmptyHolder) holder).bind();
        }
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    class ColorHolder extends RecyclerView.ViewHolder {
        final MaterialButtonToggleGroup group;
        final TextView summary;
        boolean binding;

        ColorHolder(@NonNull View itemView) {
            super(itemView);
            group = itemView.findViewById(R.id.color_mode_group);
            summary = itemView.findViewById(R.id.color_mode_summary);
            group.addOnButtonCheckedListener((g, checkedId, isChecked) -> {
                if (!isChecked || binding) {
                    return;
                }
                int mode = checkedId == R.id.color_mode_light ? 1
                        : checkedId == R.id.color_mode_dark ? 2 : 0;
                bind(mode);
                callbacks.onColorMode(mode);
            });
        }

        void bind(int mode) {
            binding = true;
            group.check(mode == 1 ? R.id.color_mode_light
                    : mode == 2 ? R.id.color_mode_dark : R.id.color_mode_follow);
            binding = false;
            summary.setText(mode == 1 ? R.string.settings_color_summary_light
                    : mode == 2 ? R.string.settings_color_summary_dark
                    : R.string.settings_color_summary_follow);
        }
    }

    class SectionHolder extends RecyclerView.ViewHolder {
        final TextView title;
        final MaterialButton one;
        final MaterialButton two;

        SectionHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.section_title);
            one = itemView.findViewById(R.id.section_action_one);
            two = itemView.findViewById(R.id.section_action_two);
        }

        void bind(Row row) {
            title.setText(row.title);
            one.setVisibility(row.actions ? View.VISIBLE : View.GONE);
            two.setVisibility(row.actions ? View.VISIBLE : View.GONE);
            one.setIconResource(R.drawable.ic_upload_file);
            one.setContentDescription(R.string.settings_import_provider);
            two.setIconResource(R.drawable.ic_add);
            two.setContentDescription(R.string.settings_add_provider);
            one.setOnClickListener(v -> callbacks.onImportProvider());
            two.setOnClickListener(v -> callbacks.onAddProvider());
        }
    }

    class PreferenceHolder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView title;
        final TextView subtitle;

        PreferenceHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.pref_icon);
            title = itemView.findViewById(R.id.pref_title);
            subtitle = itemView.findViewById(R.id.pref_subtitle);
        }

        void bind(Row row) {
            icon.setImageResource(row.icon);
            title.setText(row.title);
            subtitle.setText(row.subtitle);
            itemView.setOnClickListener(v -> callbacks.onPreference(row.key));
        }
    }

    class SearchHolder extends RecyclerView.ViewHolder {
        final TextInputEditText input;

        SearchHolder(@NonNull View itemView) {
            super(itemView);
            input = itemView.findViewById(R.id.provider_search);
            input.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(android.text.Editable s) {
                    String value = s.toString();
                    if (!value.equals(query)) {
                        setQuery(value);
                    }
                }
            });
        }
    }

    class EmptyHolder extends RecyclerView.ViewHolder {
        final TextView title;

        EmptyHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.pref_title);
            itemView.findViewById(R.id.pref_icon).setVisibility(View.GONE);
            itemView.findViewById(R.id.pref_subtitle).setVisibility(View.GONE);
            itemView.findViewById(R.id.pref_chevron).setVisibility(View.GONE);
            itemView.setClickable(false);
        }

        void bind() {
            title.setText(R.string.settings_empty);
        }
    }

    class ProviderHolder extends RecyclerView.ViewHolder {
        final TextView avatar;
        final TextView name;
        final TextView subtitle;
        final ImageView chevron;
        final View body;
        final TextInputEditText baseUrl;
        final TextInputEditText apiKey;
        final MaterialButton fetch;
        ProviderItem item;

        ProviderHolder(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.provider_avatar);
            name = itemView.findViewById(R.id.provider_name);
            subtitle = itemView.findViewById(R.id.provider_subtitle);
            chevron = itemView.findViewById(R.id.provider_chevron);
            body = itemView.findViewById(R.id.provider_body);
            baseUrl = itemView.findViewById(R.id.provider_base_url);
            apiKey = itemView.findViewById(R.id.provider_api_key);
            fetch = itemView.findViewById(R.id.provider_fetch);

            itemView.findViewById(R.id.provider_header).setOnClickListener(v -> toggle());
            fetch.setOnClickListener(v -> {
                if (item != null) {
                    callbacks.onFetchModels(item);
                }
            });
        }

        void bind(ProviderItem provider) {
            item = provider;
            avatar.setText(provider.letter);
            name.setText(provider.name);
            subtitle.setText(itemView.getContext().getString(R.string.settings_provider_sub,
                    provider.models, host(provider.baseUrl)));
            body.setVisibility(provider.expanded ? View.VISIBLE : View.GONE);
            chevron.setRotation(provider.expanded ? 180f : 0f);
            chevron.setContentDescription(itemView.getContext().getString(
                    provider.expanded ? R.string.settings_cd_collapse : R.string.settings_cd_expand));
            if (!baseUrl.getText().toString().equals(provider.baseUrl)) {
                baseUrl.setText(provider.baseUrl);
            }
            if (!apiKey.getText().toString().equals(provider.apiKey)) {
                apiKey.setText(provider.apiKey);
            }
        }

        void toggle() {
            if (item == null) {
                return;
            }
            item.expanded = !item.expanded;
            if (itemView.getParent() instanceof ViewGroup) {
                TransitionManager.beginDelayedTransition((ViewGroup) itemView.getParent(),
                        new ChangeBounds().setDuration(220));
            }
            body.setVisibility(item.expanded ? View.VISIBLE : View.GONE);
            chevron.animate().rotation(item.expanded ? 180f : 0f).setDuration(200).start();
            chevron.setContentDescription(itemView.getContext().getString(
                    item.expanded ? R.string.settings_cd_collapse : R.string.settings_cd_expand));
        }
    }

    private static String host(String baseUrl) {
        try {
            java.net.URI uri = java.net.URI.create(baseUrl);
            String host = uri.getHost();
            return host == null ? baseUrl : host;
        } catch (IllegalArgumentException e) {
            return baseUrl;
        }
    }

    private static class RowDiff extends DiffUtil.Callback {
        private final List<Row> oldRows;
        private final List<Row> newRows;

        RowDiff(List<Row> oldRows, List<Row> newRows) {
            this.oldRows = new ArrayList<>(oldRows);
            this.newRows = newRows;
        }

        @Override
        public int getOldListSize() {
            return oldRows.size();
        }

        @Override
        public int getNewListSize() {
            return newRows.size();
        }

        @Override
        public boolean areItemsTheSame(int oldPos, int newPos) {
            Row a = oldRows.get(oldPos);
            Row b = newRows.get(newPos);
            if (a.type != b.type) {
                return false;
            }
            if (a.type == TYPE_PROVIDER) {
                return a.provider == b.provider;
            }
            if (a.type == TYPE_SECTION) {
                return android.text.TextUtils.equals(a.title, b.title);
            }
            if (a.type == TYPE_PREFERENCE) {
                return a.key != null && a.key.equals(b.key);
            }
            return true;
        }

        @Override
        public boolean areContentsTheSame(int oldPos, int newPos) {
            Row a = oldRows.get(oldPos);
            Row b = newRows.get(newPos);
            return a.actions == b.actions
                    && a.icon == b.icon
                    && android.text.TextUtils.equals(a.title, b.title)
                    && android.text.TextUtils.equals(a.subtitle, b.subtitle);
        }
    }
}
