package dev.neko.chatdemo.ui;

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

    public final int id;
    public final int type;
    public final String meta;
    public final String time;
    public final String author;
    public final String code;

    /** Mutable: a user message can be reopened for editing and written back in place. */
    public String text;

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
