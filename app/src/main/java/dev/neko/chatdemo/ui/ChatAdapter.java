package dev.neko.chatdemo.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import dev.neko.chatdemo.R;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.Holder> {

    private final List<ChatItem> items = new ArrayList<>();

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
