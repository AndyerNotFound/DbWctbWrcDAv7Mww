package dev.neko.chatdemo.ui;

import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import dev.neko.chatdemo.R;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.Holder> {

    /** What the chat list tells the composer about a user bubble the finger landed on. */
    public interface MessageListener {
        /** A tap: the message goes back into the field for editing. */
        void onEditMessage(@NonNull ChatItem item);

        /** A long press: the action card opens under the bubble. */
        void onMessageActions(@NonNull View bubble, @NonNull ChatItem item);
    }

    private final List<ChatItem> items = new ArrayList<>();
    @Nullable
    private MessageListener listener;
    private int editingId = -1;

    public void setListener(@Nullable MessageListener listener) {
        this.listener = listener;
    }

    public void submit(List<ChatItem> seed) {
        items.clear();
        items.addAll(seed);
        notifyDataSetChanged();
    }

    public void add(ChatItem item) {
        items.add(item);
        notifyItemInserted(items.size() - 1);
    }

    public int count() {
        return items.size();
    }

    @Nullable
    public ChatItem findById(int id) {
        for (ChatItem item : items) {
            if (item.id == id) {
                return item;
            }
        }
        return null;
    }

    /** Row id, so a caller holding a {@link ChatItem} can find its position after a rebind. */
    public int idAt(int position) {
        return items.get(position).id;
    }

    public void removeAt(int position) {
        if (position < 0 || position >= items.size()) {
            return;
        }
        items.remove(position);
        notifyItemRemoved(position);
    }

    /** Marks one message as the one being edited, so its bubble can show the state. */
    public void setEditingId(int id) {
        if (editingId == id) {
            return;
        }
        int previous = indexOf(editingId);
        editingId = id;
        if (previous >= 0) {
            notifyItemChanged(previous);
        }
        int next = indexOf(id);
        if (next >= 0) {
            notifyItemChanged(next);
        }
    }

    private int indexOf(int id) {
        if (id < 0) {
            return -1;
        }
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).id == id) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).type;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout;
        switch (viewType) {
            case ChatItem.TYPE_DIVIDER:
                layout = R.layout.item_chat_divider;
                break;
            case ChatItem.TYPE_USER:
                layout = R.layout.item_chat_user;
                break;
            case ChatItem.TYPE_TOOL:
                layout = R.layout.item_chat_tool;
                break;
            case ChatItem.TYPE_CODE:
                layout = R.layout.item_chat_code;
                break;
            case ChatItem.TYPE_IMAGE:
                layout = R.layout.item_chat_image;
                break;
            case ChatItem.TYPE_CHIPS:
                layout = R.layout.item_chat_chips;
                break;
            case ChatItem.TYPE_NEKO:
            default:
                layout = R.layout.item_chat_neko;
                break;
        }
        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        ChatItem item = items.get(position);

        TextView text = holder.itemView.findViewById(R.id.message_text);
        if (text != null && item.text != null) {
            text.setText(item.text);
        }

        TextView divider = holder.itemView.findViewById(R.id.divider_text);
        if (divider != null && item.text != null) {
            divider.setText(item.text);
        }

        TextView time = holder.itemView.findViewById(R.id.message_time);
        if (time != null && item.time != null) {
            time.setText(item.time);
        }

        TextView meta = holder.itemView.findViewById(R.id.message_meta);
        if (meta != null && item.meta != null) {
            meta.setText(item.meta);
        }

        TextView author = holder.itemView.findViewById(R.id.message_author);
        if (author != null && item.author != null) {
            author.setText(item.author);
        }

        TextView code = holder.itemView.findViewById(R.id.code_text);
        if (code != null && item.code != null) {
            code.setText(item.code);
        }

        View bubble = holder.itemView.findViewById(R.id.message_bubble);
        if (bubble instanceof BubbleButton) {
            BubbleButton button = (BubbleButton) bubble;
            bindBubble(button, item);
        }
    }

    /**
     * The bubble is a button: one tap reopens the message in the composer, a long press opens the
     * action card, and while it is the message being edited the container switches to the tertiary
     * pair so the row is recognisable from the composer.
     */
    private void bindBubble(@NonNull BubbleButton bubble, @NonNull ChatItem item) {
        boolean editing = item.id == editingId;
        bubble.setContainerColor(containerFor(bubble, editing));

        // The content colours follow the container pair, so an edited bubble stays readable.
        int onContainer = com.google.android.material.color.MaterialColors.getColor(bubble,
                editing ? com.google.android.material.R.attr.colorOnTertiaryContainer
                        : com.google.android.material.R.attr.colorOnPrimaryContainer);
        View text = bubble.findViewById(R.id.message_text);
        if (text instanceof TextView) {
            ((TextView) text).setTextColor(onContainer);
        }

        if (!item.isEditable()) {
            bubble.setClickable(false);
            bubble.setLongClickable(false);
            bubble.setOnClickListener(null);
            bubble.setOnLongClickListener(null);
            return;
        }

        bubble.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditMessage(item);
            }
        });
        bubble.setOnLongClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            if (listener != null) {
                listener.onMessageActions(v, item);
            }
            return true;
        });
    }

    private int containerFor(@NonNull View view, boolean editing) {
        return com.google.android.material.color.MaterialColors.getColor(view,
                editing ? com.google.android.material.R.attr.colorTertiaryContainer
                        : com.google.android.material.R.attr.colorPrimaryContainer);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        Holder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
