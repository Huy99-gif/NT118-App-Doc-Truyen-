package com.example.android_app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android_app.R;
import com.example.android_app.model.CommentResponse;
import com.example.android_app.util.TimeFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {
    public interface Listener {
        void onDeleteComment(CommentResponse comment);
    }

    private final List<CommentResponse> comments = new ArrayList<>();
    private final Listener listener;

    public CommentAdapter(Listener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        holder.bind(comments.get(position));
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    public void setItems(List<CommentResponse> items) {
        comments.clear();
        if (items != null) comments.addAll(items);
        notifyDataSetChanged();
    }

    public void addItems(List<CommentResponse> items) {
        if (items == null) return;
        int start = comments.size();
        for (CommentResponse item : items) {
            // bình luận mới đăng làm lệch offset phân trang, bỏ qua id đã có để không bị trùng
            if (!containsId(item.id)) comments.add(item);
        }
        notifyItemRangeInserted(start, comments.size() - start);
    }

    public void prepend(CommentResponse comment) {
        comments.add(0, comment);
        notifyItemInserted(0);
    }

    public void removeById(long id) {
        for (int i = 0; i < comments.size(); i++) {
            Long current = comments.get(i).id;
            if (current != null && current == id) {
                comments.remove(i);
                notifyItemRemoved(i);
                return;
            }
        }
    }

    private boolean containsId(Long id) {
        if (id == null) return false;
        for (CommentResponse comment : comments) {
            if (id.equals(comment.id)) return true;
        }
        return false;
    }

    class CommentViewHolder extends RecyclerView.ViewHolder {
        private final TextView author;
        private final TextView time;
        private final TextView content;
        private final ImageButton delete;

        CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            author = itemView.findViewById(R.id.tvCommentAuthor);
            time = itemView.findViewById(R.id.tvCommentTime);
            content = itemView.findViewById(R.id.tvCommentContent);
            delete = itemView.findViewById(R.id.btnDeleteComment);
        }

        void bind(CommentResponse comment) {
            author.setText(comment.displayName == null ? "Người đọc" : comment.displayName);
            time.setText(TimeFormatter.formatIsoInstant(comment.createdAt, TimeZone.getDefault()));
            content.setText(comment.content);
            // chỉ chủ bình luận mới thấy nút xoá (server tính sẵn cờ mine)
            boolean mine = Boolean.TRUE.equals(comment.mine);
            delete.setVisibility(mine ? View.VISIBLE : View.GONE);
            delete.setOnClickListener(mine ? v -> {
                if (listener != null) listener.onDeleteComment(comment);
            } : null);
        }
    }
}
