package dev.neko.chatdemo.sheet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Calls the reasoning sheet pages make back into their host. */
interface ReasoningPageHost {
    void onOpenModels();

    void onEffortChanged(int index, boolean fromUser);

    void onOpenCustomEffort();

    void onModelPicked(@NonNull String name, @NonNull String meta);

    void onModelsChanged(int providerIndex, @Nullable String group, @NonNull String providerName,
                         int count);
}
