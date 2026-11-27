package dev.neko.chatdemo.ui;

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
        list.setAdapter(adapter);
        adapter.submit(seed());
    }

    private List<ChatItem> seed() {
        List<ChatItem> items = new ArrayList<>();
        items.add(ChatItem.divider("Today · 23:41"));
        items.add(ChatItem.neko(
                "Evening. I pulled the MD3 sources into the workspace — buttons, tabs, sheets, slider. The split button "
                        + "is real in 1.14, so we do not have to fake it.",
                "Neko · GPT-5.4 · 23:41"));
        items.add(ChatItem.user(
                "Good. I want the chat shell to feel like a phone app, not a component gallery. Top bar with tabs, "
                        + "input dock pinned to the bottom.",
                "23:42"));
        items.add(ChatItem.neko(
                "Then the dock is the anchor. Context progress on top, a six-line field in the middle, tools at the "
                        + "bottom. The send action stays tonal and breathes into a stop control while generating.",
                "12s · 214 tokens"));
        items.add(ChatItem.tool(
                "I checked the XML for the pieces we need. Everything is public in 1.14.0 — no copied styles.",
                "3 tool calls · 14s"));
        items.add(ChatItem.code(
                "The insets are the fiddly part. This is the shape of the activity:",
                "setDecorFitsSystemWindows(window, false);\n"
                        + "ViewCompat.setOnApplyWindowInsetsListener(root, (v, i) -> {\n"
                        + "  Insets bars = i.getInsets(systemBars() | ime());\n"
                        + "  dock.setPadding(8, 8, 8, 8 + bars.bottom);\n"
                        + "  return i;\n"
                        + "});",
                "MainActivity.java · 6 lines"));
        items.add(ChatItem.image(
                "This is the dock I sketched. Keep the rounded top, lose the divider.",
                "23:47"));
        items.add(ChatItem.chips(
                "I can take the next pass in a few directions."));
        items.add(ChatItem.divider("23:52"));
        items.add(ChatItem.neko(
                "The map is live, by the way. Open Chat Map and you can still see the branch where we tried pill tabs.",
                "4s · 86 tokens"));
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
        adapter.add(ChatItem.neko(text, "just now · demo reply"));
        scrollToBottom();
    }

    public void resetConversation() {
        if (adapter == null) {
            return;
        }
        List<ChatItem> items = new ArrayList<>();
        items.add(ChatItem.divider("Today · now"));
        items.add(ChatItem.neko("Fresh page. What are we building?", "just now · new session"));
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
