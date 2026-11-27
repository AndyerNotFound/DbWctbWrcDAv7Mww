package dev.neko.chatdemo.ui;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.ChangeBounds;
import androidx.transition.TransitionManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import dev.neko.chatdemo.R;

/** Provider list for the providers page: search row first, then the collapsible cards. */
public class ProvidersAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface Callbacks {
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

    private static final int TYPE_SEARCH = 0;
    private static final int TYPE_PROVIDER = 1;
    private static final int TYPE_EMPTY = 2;

    private final Context context;
    private final List<ProviderItem> providers;
    private final List<ProviderItem> filtered = new ArrayList<>();
    private final Callbacks callbacks;
    private String query = "";

    public ProvidersAdapter(@NonNull Context context, @NonNull List<ProviderItem> providers,
                            @NonNull Callbacks callbacks) {
        this.context = context;
        this.providers = providers;
        this.callbacks = callbacks;
        applyFilter();
    }

    private void applyFilter() {
        filtered.clear();
        for (ProviderItem provider : providers) {
            if (query.isEmpty()
                    || provider.name.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT))) {
                filtered.add(provider);
            }
        }
    }

    private int contentCount() {
        return filtered.isEmpty() ? 1 : filtered.size();
    }

    public boolean isProviderPosition(int position) {
        return position >= 1 && position <= filtered.size();
    }

    public boolean move(int from, int to) {
        if (!isProviderPosition(from) || !isProviderPosition(to)) {
            return false;
        }
        ProviderItem a = filtered.get(from - 1);
        ProviderItem b = filtered.get(to - 1);
        Collections.swap(providers, providers.indexOf(a), providers.indexOf(b));
        Collections.swap(filtered, from - 1, to - 1);
        notifyItemMoved(from, to);
        return true;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return TYPE_SEARCH;
        }
        return filtered.isEmpty() ? TYPE_EMPTY : TYPE_PROVIDER;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case TYPE_SEARCH:
                return new SearchHolder(inflater.inflate(R.layout.item_settings_search, parent, false));
            case TYPE_EMPTY:
                return new EmptyHolder(inflater.inflate(R.layout.item_providers_empty, parent, false));
            case TYPE_PROVIDER:
            default:
                return new ProviderHolder(inflater.inflate(R.layout.item_settings_provider, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ProviderHolder && isProviderPosition(position)) {
            ((ProviderHolder) holder).bind(filtered.get(position - 1));
        }
    }

    @Override
    public int getItemCount() {
        return 1 + contentCount();
    }

    class SearchHolder extends RecyclerView.ViewHolder {
        final TextInputEditText input;

        SearchHolder(@NonNull View itemView) {
            super(itemView);
            input = itemView.findViewById(R.id.provider_search);
            input.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    String value = s.toString();
                    if (value.equals(query)) {
                        return;
                    }
                    int previous = contentCount();
                    query = value;
                    applyFilter();
                    notifyItemRangeRemoved(1, previous);
                    notifyItemRangeInserted(1, contentCount());
                }
            });
        }
    }

    class EmptyHolder extends RecyclerView.ViewHolder {
        EmptyHolder(@NonNull View itemView) {
            super(itemView);
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

            itemView.findViewById(R.id.provider_header).setOnClickListener(v -> toggle());
            itemView.findViewById(R.id.provider_fetch).setOnClickListener(v -> {
                if (item != null) {
                    callbacks.onFetchModels(item);
                }
            });
        }

        void bind(ProviderItem provider) {
            item = provider;
            avatar.setText(provider.letter);
            name.setText(provider.name);
            subtitle.setText(context.getString(R.string.settings_provider_sub,
                    provider.models, host(provider.baseUrl)));
            body.setVisibility(provider.expanded ? View.VISIBLE : View.GONE);
            chevron.setRotation(provider.expanded ? 180f : 0f);
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
}
