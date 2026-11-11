package com.example.android_app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android_app.R;
import com.example.android_app.model.BookmarkResponse;

import java.util.ArrayList;
import java.util.List;

public class BookmarkAdapter extends RecyclerView.Adapter<BookmarkAdapter.BookmarkViewHolder> {
    public interface Listener {
        void onBookmarkClick(BookmarkResponse bookmark);
        void onDeleteBookmark(BookmarkResponse bookmark);
    }

    private final List<BookmarkResponse> bookmarks = new ArrayList<>();
    private final Listener listener;

    public BookmarkAdapter(Listener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public BookmarkViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_bookmark, parent, false);
        return new BookmarkViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookmarkViewHolder holder, int position) {
        holder.bind(bookmarks.get(position));
    }

    @Override
    public int getItemCount() {
        return bookmarks.size();
    }

    public void setBookmarks(List<BookmarkResponse> items) {
        bookmarks.clear();
        if (items != null) bookmarks.addAll(items);
        notifyDataSetChanged();
    }

    class BookmarkViewHolder extends RecyclerView.ViewHolder {
        private final TextView title;
        private final TextView details;
        private final TextView note;
        private final ImageButton delete;

        BookmarkViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tvBookmarkStory);
            details = itemView.findViewById(R.id.tvBookmarkChapter);
            note = itemView.findViewById(R.id.tvBookmarkNote);
            delete = itemView.findViewById(R.id.btnDeleteBookmark);
        }

        void bind(BookmarkResponse bookmark) {
            title.setText(bookmark.storyTitle == null ? "Truyện" : bookmark.storyTitle);
            String chapter = "Chương " + (bookmark.chapterNo == null ? "?" : bookmark.chapterNo)
                    + (bookmark.chapterTitle == null ? "" : ": " + bookmark.chapterTitle);
            details.setText(chapter);
            note.setText(bookmark.note == null || bookmark.note.trim().isEmpty()
                    ? "Vị trí: " + (bookmark.charOffset == null ? 0 : bookmark.charOffset)
                    : bookmark.note);
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onBookmarkClick(bookmark);
            });
            delete.setOnClickListener(v -> {
                if (listener != null) listener.onDeleteBookmark(bookmark);
            });
        }
    }
}
