package com.example.android_app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android_app.R;
import com.example.android_app.model.Chapter;

import java.util.ArrayList;
import java.util.List;

public class ChapterAdapter extends RecyclerView.Adapter<ChapterAdapter.ChapterViewHolder> {

    private final List<Chapter> chapterList = new ArrayList<>();
    private final OnChapterClickListener listener;

    public interface OnChapterClickListener {
        void onChapterClick(Chapter chapter);
    }

    public ChapterAdapter(OnChapterClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ChapterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chapter, parent, false);
        return new ChapterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChapterViewHolder holder, int position) {
        Chapter chapter = chapterList.get(position);
        holder.bind(chapter);
    }

    @Override
    public int getItemCount() {
        return chapterList.size();
    }

    public void setChapters(List<Chapter> chapters) {
        this.chapterList.clear();
        if (chapters != null) {
            this.chapterList.addAll(chapters);
        }
        notifyDataSetChanged();
    }

    class ChapterViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvChapterTitle;
        private final TextView tvChapterSub;

        ChapterViewHolder(@NonNull View itemView) {
            super(itemView);
            tvChapterTitle = itemView.findViewById(R.id.tvChapterTitle);
            tvChapterSub = itemView.findViewById(R.id.tvChapterSub);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onChapterClick(chapterList.get(pos));
                }
            });
        }

        void bind(Chapter chapter) {
            String titleStr = "Chương " + chapter.getChapterNumber() + ": " + chapter.getTitle();
            tvChapterTitle.setText(titleStr);
            String subStr = "Cập nhật: " + chapter.getUpdatedDate() + "  •  " + chapter.getViewCount() + " lượt đọc";
            tvChapterSub.setText(subStr);
        }
    }
}
