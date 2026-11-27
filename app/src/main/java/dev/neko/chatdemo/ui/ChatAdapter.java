package dev.neko.chatdemo.ui;

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

    /** What the chat list tells the composer about a bubble the finger landed on. */
    public interface MessageListener {
        /** A tap: the message goes back into the field for editing. */
        void onEditMessage(@NonNull ChatItem item);

        /** A long press: the action card opens under the bubble. */
        void onMessageActions(@NonNull View bubble, @NonNull ChatItem item);

        /** A footer action of an assistant reply: copy, regenerate, share, more. */
        void onAssistantAction(@NonNull View anchor, @NonNull ChatItem item, int action);
    }

    public static final int ACTION_COPY = 0;
    public static final int ACTION_RETRY = 1;
    public static final int ACTION_SHARE = 2;
    public static final int ACTION_MORE = 3;

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
            case ChatItem.TYPE_IMAGE:
                layout = R.layout.item_chat_image;
                break;
            case ChatItem.TYPE_NEKO:
            case ChatItem.TYPE_TOOL:
            case ChatItem.TYPE_CODE:
            case ChatItem.TYPE_CHIPS:
            default:
                // Every assistant row is the same turn layout: the parts it does not need stay gone.
                layout = R.layout.item_chat_assistant;
                break;
        }
        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        ChatItem item = items.get(position);

        if (item.isAssistant()) {
            bindAssistant(holder, item);
        } else {
            bindPlain(holder, item);
        }

        View bubble = holder.itemView.findViewById(R.id.message_bubble);
        if (bubble instanceof BubbleButton) {
            bindBubble((BubbleButton) bubble, item);
        }
    }

    private void bindPlain(@NonNull Holder holder, @NonNull ChatItem item) {
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
    }

    /**
     * The assistant turn: one steps card on top, the latest paragraph of the body, an optional chip
     * that unfolds the earlier paragraphs, and a footer with the actions and the stats.
     */
    private void bindAssistant(@NonNull Holder holder, @NonNull ChatItem item) {
        TextView author = holder.itemView.findViewById(R.id.message_author);
        if (author != null && item.author != null) {
            author.setText(item.author);
        }

        StepCardView steps = holder.itemView.findViewById(R.id.steps_card);
        if (steps != null) {
            steps.setExpandListener(expanded -> item.stepsExpanded = expanded);
            steps.bind(item.steps, item.stepsExpanded);
        }

        // Standard icon buttons sit edge to edge inside the pill; the group's default spacing would
        // pull them apart and break the single capsule.
        View actions = holder.itemView.findViewById(R.id.assistant_actions);
        if (actions instanceof com.google.android.material.button.MaterialButtonGroup) {
            ((com.google.android.material.button.MaterialButtonGroup) actions).setSpacing(0);
        }

        TextView text = holder.itemView.findViewById(R.id.message_text);
        if (text != null) {
            text.setText(item.latestParagraph());
        }

        TextView earlier = holder.itemView.findViewById(R.id.message_earlier);
        TextView toggle = holder.itemView.findViewById(R.id.earlier_toggle);
        String earlierText = item.earlierParagraphs();
        if (earlier != null && toggle != null) {
            if (earlierText == null) {
                earlier.setVisibility(View.GONE);
                toggle.setVisibility(View.GONE);
            } else {
                earlier.setText(earlierText);
                earlier.setVisibility(item.earlierExpanded ? View.VISIBLE : View.GONE);
                toggle.setVisibility(View.VISIBLE);
                toggle.setText(item.earlierExpanded
                        ? R.string.earlier_collapse
                        : R.string.earlier_expand);
                toggle.setOnClickListener(v -> {
                    item.earlierExpanded = !item.earlierExpanded;
                    int position = holder.getBindingAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        notifyItemChanged(position);
                    }
                });
            }
        }

        TextView code = holder.itemView.findViewById(R.id.code_text);
        View codeBlock = holder.itemView.findViewById(R.id.code_block);
        if (codeBlock != null) {
            if (item.code == null) {
                codeBlock.setVisibility(View.GONE);
            } else {
                codeBlock.setVisibility(View.VISIBLE);
                if (code != null) {
                    code.setText(item.code);
                }
            }
        }

        View toolCard = holder.itemView.findViewById(R.id.tool_card);
        if (toolCard != null) {
            toolCard.setVisibility(item.type == ChatItem.TYPE_TOOL ? View.VISIBLE : View.GONE);
        }

        View chips = holder.itemView.findViewById(R.id.suggestion_group);
        if (chips != null) {
            chips.setVisibility(item.type == ChatItem.TYPE_CHIPS ? View.VISIBLE : View.GONE);
        }

        TextView stats = holder.itemView.findViewById(R.id.message_stats);
        if (stats != null) {
            String line = item.stats != null ? item.stats : item.meta;
            if (line == null || line.isEmpty()) {
                stats.setVisibility(View.GONE);
            } else {
                stats.setVisibility(View.VISIBLE);
                stats.setText(line);
            }
        }

        bindAction(holder.itemView.findViewById(R.id.action_copy), item, ACTION_COPY);
        bindAction(holder.itemView.findViewById(R.id.action_retry), item, ACTION_RETRY);
        bindAction(holder.itemView.findViewById(R.id.action_share), item, ACTION_SHARE);
        bindAction(holder.itemView.findViewById(R.id.action_more), item, ACTION_MORE);
    }

    private void bindAction(@Nullable View button, @NonNull ChatItem item, int action) {
        if (button == null) {
            return;
        }
        button.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAssistantAction(v, item, action);
            }
        });
    }

    /**
     * The bubble is a button: one tap reopens the message in the composer and a long press opens
     * the action card. While it is the message being edited the container switches to the tertiary
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
