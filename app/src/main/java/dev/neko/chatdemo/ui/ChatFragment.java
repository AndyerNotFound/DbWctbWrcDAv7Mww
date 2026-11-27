package dev.neko.chatdemo.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import dev.neko.chatdemo.R;

public class ChatFragment extends Fragment {

    /** The activity owns the composer, so the chat list reports taps and actions upward. */
    public interface Host {
        void onEditMessage(@NonNull ChatItem item);

        void onMessageActions(@NonNull View bubble, @NonNull ChatItem item);

        void onAssistantAction(@NonNull View anchor, @NonNull ChatItem item, int action);
    }

    private RecyclerView list;
    private ChatAdapter adapter;

    public ChatFragment() {
        super(R.layout.content_chat);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        list = view.findViewById(R.id.chat_list);
        LinearLayoutManager lm = new LinearLayoutManager(requireContext());
        list.setLayoutManager(lm);
        adapter = new ChatAdapter();
        adapter.setListener(new ChatAdapter.MessageListener() {
            @Override
            public void onEditMessage(@NonNull ChatItem item) {
                Host host = host();
                if (host != null) {
                    host.onEditMessage(item);
                }
            }

            @Override
            public void onMessageActions(@NonNull View bubble, @NonNull ChatItem item) {
                Host host = host();
                if (host != null) {
                    host.onMessageActions(bubble, item);
                }
            }

            @Override
            public void onAssistantAction(@NonNull View anchor, @NonNull ChatItem item, int action) {
                Host host = host();
                if (host != null) {
                    host.onAssistantAction(anchor, item, action);
                }
            }
        });
        list.setAdapter(adapter);
        adapter.submit(seed());
    }

    @Nullable
    private Host host() {
        Context context = getContext();
        return context instanceof Host ? (Host) context : null;
    }

    private List<ChatItem> seed() {
        List<ChatItem> items = new ArrayList<>();
        items.add(ChatItem.divider("Today · 23:41"));

        items.add(ChatItem.neko(
                "Evening. I pulled the MD3 sources into the workspace — buttons, tabs, sheets, slider. "
                        + "The split button is real in 1.14, so we do not have to fake it.",
                "Neko · GPT-5.4 · 23:41")
                .withStats("4s · 86 tokens · GPT-5.4"));

        items.add(ChatItem.user(
                "Good. I want the chat shell to feel like a phone app, not a component gallery. Top bar with tabs, "
                        + "input dock pinned to the bottom.",
                "23:42"));

        items.add(ChatItem.neko(
                "Then the dock is the anchor. Context progress on top, a six-line field in the middle, tools at the "
                        + "bottom. The send action stays tonal and breathes into a stop control while generating.\n\n"
                        + "I also moved the insets out of the field and into the dock container, so the keyboard "
                        + "lifts the whole panel instead of only the text row. That is what keeps the send button "
                        + "under the thumb when the field grows.\n\n"
                        + "The one thing I could not settle by reading the sources is how the dock should behave "
                        + "while a sheet is open — pinned, or pushed back. Say the word and I take the pinned route.",
                "12s · 214 tokens")
                .withSteps(
                        new ChatItem.Step(R.drawable.ic_psychology, "Reasoning · high effort",
                                "1,840 thinking tokens"),
                        new ChatItem.Step(R.drawable.ic_build, "Read · MainActivity.java",
                                "825 lines · 18 ms"),
                        new ChatItem.Step(R.drawable.ic_build, "Read · activity_main.xml",
                                "412 lines · 12 ms"),
                        new ChatItem.Step(R.drawable.ic_search, "Searched · insets, ime",
                                "3 results · Material 3 docs"),
                        new ChatItem.Step(R.drawable.ic_code, "Wrote · dock insets",
                                "6 lines added"))
                .withStats("12s · 214 tokens · GPT-5.4"));

        items.add(ChatItem.tool(
                "I checked the XML for the pieces we need. Everything is public in 1.14.0 — no copied styles.",
                "3 tool calls · 14s")
                .withSteps(
                        new ChatItem.Step(R.drawable.ic_build, "Read · 3 files",
                                "MaterialSplitButton, ShapeAppearanceOverlay, activity_main"),
                        new ChatItem.Step(R.drawable.ic_check_circle, "Verified · no private styles",
                                "1.14.0 · all public"))
                .withStats("3 tool calls · 14s · 512 tokens"));

        items.add(ChatItem.code(
                "The insets are the fiddly part. This is the shape of the activity:",
                "setDecorFitsSystemWindows(window, false);\n"
                        + "ViewCompat.setOnApplyWindowInsetsListener(root, (v, i) -> {\n"
                        + "  Insets bars = i.getInsets(systemBars() | ime());\n"
                        + "  dock.setPadding(8, 8, 8, 8 + bars.bottom);\n"
                        + "  return i;\n"
                        + "});",
                "MainActivity.java · 6 lines")
                .withSteps(
                        new ChatItem.Step(R.drawable.ic_code, "Wrote · window insets",
                                "MainActivity.java · 6 lines"))
                .withStats("6s · 148 tokens · GPT-5.4"));

        items.add(ChatItem.image(
                "This is the dock I sketched. Keep the rounded top, lose the divider.",
                "23:47"));

        items.add(ChatItem.chips(
                "I can take the next pass in a few directions.")
                .withStats("2s · 34 tokens"));

        items.add(ChatItem.divider("23:52"));

        items.add(ChatItem.neko(
                "The map is live, by the way. Open Chat Map and you can still see the branch where we tried pill tabs.",
                "4s · 86 tokens")
                .withSteps(
                        new ChatItem.Step(R.drawable.ic_alt_route, "Branched · pill tabs",
                                "kept as a side branch"))
                .withStats("4s · 86 tokens · GPT-5.4"));

        items.add(ChatItem.user(
                "Perfect. Then let's ship the demo build.",
                "23:53"));
        return items;
    }

    public void addUserMessage(String text) {
        if (adapter == null) {
            return;
        }
        adapter.add(ChatItem.user(text, "now"));
        scrollToBottom();
    }

    public void addAssistantMessage(String text) {
        if (adapter == null) {
            return;
        }
        adapter.add(ChatItem.neko(text, "just now · demo reply")
                .withStats("2s · 64 tokens · GPT-5.4"));
        scrollToBottom();
    }

    /** Writes an edited message back into the row it came from. */
    public void updateUserMessage(@NonNull ChatItem item, @NonNull String text) {
        if (adapter == null) {
            return;
        }
        item.text = text;
        ChatItem stored = adapter.findById(item.id);
        if (stored != null) {
            stored.text = text;
        }
        int index = indexOf(item);
        if (index >= 0) {
            adapter.notifyItemChanged(index);
        }
    }

    public void removeMessage(@NonNull ChatItem item) {
        if (adapter == null) {
            return;
        }
        int index = indexOf(item);
        if (index >= 0) {
            adapter.removeAt(index);
        }
    }

    /** Paints the bubble of the message that is currently in the composer. */
    public void setEditingMessage(int id) {
        if (adapter != null) {
            adapter.setEditingId(id);
        }
    }

    private int indexOf(@NonNull ChatItem item) {
        if (list == null || adapter == null) {
            return -1;
        }
        for (int i = 0; i < adapter.getItemCount(); i++) {
            if (adapter.idAt(i) == item.id) {
                return i;
            }
        }
        return -1;
    }

    public void resetConversation() {
        if (adapter == null) {
            return;
        }
        List<ChatItem> items = new ArrayList<>();
        items.add(ChatItem.divider("Today · now"));
        items.add(ChatItem.neko("Fresh page. What are we building?", "just now · new session")
                .withStats("1s · 12 tokens"));
        adapter.submit(items);
        scrollToBottom();
    }

    public void scrollToBottom() {
        if (list == null || adapter == null) {
            return;
        }
        list.post(() -> {
            if (adapter.getItemCount() > 0) {
                list.scrollToPosition(adapter.getItemCount() - 1);
            }
        });
    }
}
