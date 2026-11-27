package dev.neko.chatdemo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

import dev.neko.chatdemo.ui.ProvidersAdapter;
import dev.neko.chatdemo.ui.SettingsUi;
import dev.neko.chatdemo.ui.UiTransitions;

/** Provider management: import/add in the top bar, search, collapsible cards, drag ordering. */
public class ProvidersActivity extends AppCompatActivity implements ProvidersAdapter.Callbacks {

    public static void start(Activity from) {
        UiTransitions.start(from, new Intent(from, ProvidersActivity.class), true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SettingsUi.setupWindow(this);
        UiTransitions.incoming(this, true);
        setContentView(R.layout.activity_providers);
        SettingsUi.applyInsets(findViewById(R.id.providers_root),
                findViewById(R.id.providers_top_bar), findViewById(R.id.providers_list));

        findViewById(R.id.providers_back).setOnClickListener(v -> finish());
        findViewById(R.id.providers_import).setOnClickListener(v ->
                notice(getString(R.string.settings_notice_import)));
        findViewById(R.id.providers_add).setOnClickListener(v ->
                notice(getString(R.string.settings_notice_add)));

        List<ProvidersAdapter.ProviderItem> providers = buildProviders();
        RecyclerView list = findViewById(R.id.providers_list);
        ProvidersAdapter adapter = new ProvidersAdapter(this, providers, this);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
        new ItemTouchHelper(new DragCallback(adapter)).attachToRecyclerView(list);
    }

    private List<ProvidersAdapter.ProviderItem> buildProviders() {
        List<ProvidersAdapter.ProviderItem> items = new ArrayList<>();
        items.add(new ProvidersAdapter.ProviderItem(
                "OpenAI", "O", "https://api.openai.com", "sk-live-1234", 12));
        items.add(new ProvidersAdapter.ProviderItem(
                "Anthropic", "A", "https://api.anthropic.com", "sk-ant-5678", 8));
        items.add(new ProvidersAdapter.ProviderItem(
                "Google", "G", "https://generativelanguage.googleapis.com", "AIza-9012", 5));
        items.add(new ProvidersAdapter.ProviderItem(
                "OpenRouter", "R", "https://openrouter.ai/api", "sk-or-3456", 42));
        items.add(new ProvidersAdapter.ProviderItem(
                "Local", "L", "http://127.0.0.1:8080", "none", 3));
        return items;
    }

    @Override
    public void onFetchModels(@NonNull ProvidersAdapter.ProviderItem provider) {
        notice(getString(R.string.settings_notice_models, provider.baseUrl));
    }

    private void notice(String message) {
        View root = findViewById(R.id.providers_root);
        Snackbar.make(root, message, Snackbar.LENGTH_SHORT).show();
    }

    private static class DragCallback extends ItemTouchHelper.SimpleCallback {
        private final ProvidersAdapter adapter;

        DragCallback(ProvidersAdapter adapter) {
            super(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
            this.adapter = adapter;
        }

        @Override
        public int getMovementFlags(@NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder) {
            int drag = adapter.isProviderPosition(viewHolder.getBindingAdapterPosition())
                    ? ItemTouchHelper.UP | ItemTouchHelper.DOWN : 0;
            return makeMovementFlags(drag, 0);
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView,
                              @NonNull RecyclerView.ViewHolder viewHolder,
                              @NonNull RecyclerView.ViewHolder target) {
            return adapter.move(viewHolder.getBindingAdapterPosition(),
                    target.getBindingAdapterPosition());
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        }

        @Override
        public boolean isLongPressDragEnabled() {
            return true;
        }
    }
}
