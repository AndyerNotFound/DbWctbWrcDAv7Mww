package dev.neko.chatdemo.ui;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/** A single row in the demo conversation. */
public class ChatItem {

    public static final int TYPE_DIVIDER = 0;
    public static final int TYPE_USER = 1;
    public static final int TYPE_NEKO = 2;
    public static final int TYPE_TOOL = 3;
    public static final int TYPE_CODE = 4;
    public static final int TYPE_IMAGE = 5;
    public static final int TYPE_CHIPS = 6;

    private static int nextId = 1;

    /**
     * One thing the assistant did before it spoke: a reasoning pass, a tool call, a read. They all
     * live in the same card, so a turn is one block instead of a stack of separate bubbles.
     */
    public static final class Step {
        @DrawableRes
        public final int icon;
        public final String title;
        /** The duration printed at the end of the meter, e.g. "7.3s". */
        public final String meta;
        /** Relative weight of this step, used to size the meter against the longest one shown. */
        public final float progress;
        /** Whether the step opens something of its own, which earns it a trailing chevron. */
        public final boolean expandable;

        public Step(@DrawableRes int icon, @NonNull String title, String meta) {
            this(icon, title, meta, 1f, false);
        }

        public Step(@DrawableRes int icon, @NonNull String title, String meta, float progress,
                    boolean expandable) {
            this.icon = icon;
            this.title = title;
            this.meta = meta;
            this.progress = progress;
            this.expandable = expandable;
        }
    }

    public final int id;
    public final int type;
    public final String meta;
    public final String time;
    public final String author;
    public final String code;

    /** Everything that happened before the reply, folded into one card. */
    public final List<Step> steps = new ArrayList<>();

    /** Token and timing line under the message: "12s · 214 tokens · GPT-5.4". */
    public String stats;

    /** Mutable: a user message can be reopened for editing and written back in place. */
    public String text;

    /** View state that survives a rebind, since the list recycles rows. */
    public boolean stepsExpanded;
    /**
     * Whether the blocks before the latest paragraph are on screen. Starts shown, so the card is
     * what you meet first; the fold line above it puts them all away in one tap.
     */
    public boolean earlierExpanded = true;

    public ChatItem(int type, String text, String meta, String time, String author, String code) {
        this.id = nextId++;
        this.type = type;
        this.text = text;
        this.meta = meta;
        this.time = time;
        this.author = author;
        this.code = code;
    }

    /** True for the rows that can be reopened in the composer. */
    public boolean isEditable() {
        return type == TYPE_USER || type == TYPE_IMAGE;
    }

    /** True for the rows that carry the assistant chrome: steps, body and a footer. */
    public boolean isAssistant() {
        return type == TYPE_NEKO || type == TYPE_TOOL || type == TYPE_CODE
                || type == TYPE_CHIPS;
    }

    public ChatItem withSteps(@NonNull Step... source) {
        for (Step step : source) {
            steps.add(step);
        }
        return this;
    }

    public ChatItem withStats(String value) {
        this.stats = value;
        return this;
    }

    /** The last paragraph of the body; everything before it can be folded away. */
    @NonNull
    public String latestParagraph() {
        String[] parts = paragraphs();
        return parts.length == 0 ? "" : parts[parts.length - 1];
    }

    /** Everything before the last paragraph, or null when the body is a single paragraph. */
    public String earlierParagraphs() {
        String[] parts = paragraphs();
        if (parts.length <= 1) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < parts.length - 1; i++) {
            if (i > 0) {
                builder.append("\n\n");
            }
            builder.append(parts[i]);
        }
        return builder.toString();
    }

    private String[] paragraphs() {
        if (text == null || text.trim().isEmpty()) {
            return new String[0];
        }
        return text.trim().split("\n\\s*\n");
    }

    public static ChatItem divider(String text) {
        return new ChatItem(TYPE_DIVIDER, text, null, null, null, null);
    }

    public static ChatItem user(String text, String time) {
        return new ChatItem(TYPE_USER, text, null, time, null, null);
    }

    public static ChatItem image(String caption, String time) {
        return new ChatItem(TYPE_IMAGE, caption, null, time, null, null);
    }

    public static ChatItem neko(String text, String meta) {
        return new ChatItem(TYPE_NEKO, text, meta, null, "Neko · GPT-5.4", null);
    }

    public static ChatItem chips(String text) {
        return new ChatItem(TYPE_CHIPS, text, null, null, "Neko", null);
    }

    public static ChatItem tool(String text, String meta) {
        return new ChatItem(TYPE_TOOL, text, meta, null, "Neko · tool call", null);
    }

    public static ChatItem code(String text, String code, String meta) {
        return new ChatItem(TYPE_CODE, text, meta, null, "Neko · GPT-5.4", code);
    }
}
