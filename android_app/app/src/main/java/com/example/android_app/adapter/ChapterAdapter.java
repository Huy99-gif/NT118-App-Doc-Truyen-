package com.example.android_app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android_app.R;
import com.example.android_app.model.Chapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChapterAdapter extends RecyclerView.Adapter<ChapterAdapter.ChapterViewHolder> {

    public enum DownloadState { NONE, QUEUED, DONE }

    private final List<Chapter> chapterList = new ArrayList<>();
    private final Map<Long, DownloadState> downloadStates = new HashMap<>();
    private final OnChapterClickListener listener;
    private OnDownloadClickListener downloadListener;
    private boolean downloadEnabled = true;

    public interface OnChapterClickListener {
        void onChapterClick(Chapter chapter);
    }

    public interface OnDownloadClickListener {
        void onDownloadClick(Chapter chapter, DownloadState state);
    }

    public ChapterAdapter(OnChapterClickListener listener) {
        this.listener = listener;
    }

    public void setOnDownloadClickListener(OnDownloadClickListener downloadListener) {
        this.downloadListener = downloadListener;
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

    public void setDownloadStates(Map<Long, DownloadState> states) {
        downloadStates.clear();
        if (states != null) downloadStates.putAll(states);
        notifyDataSetChanged();
    }

    /** Khi chưa có tài khoản để gắn dữ liệu offline thì nút tải hiện mờ. */
    public void setDownloadEnabled(boolean enabled) {
        downloadEnabled = enabled;
        notifyDataSetChanged();
    }

    private DownloadState stateOf(Chapter chapter) {
        try {
            DownloadState state = downloadStates.get(Long.parseLong(chapter.getId()));
            return state == null ? DownloadState.NONE : state;
        } catch (NumberFormatException exception) {
            return DownloadState.NONE;
        }
    }

    class ChapterViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvChapterTitle;
        private final TextView tvChapterSub;
        private final ImageButton btnDownload;

        ChapterViewHolder(@NonNull View itemView) {
            super(itemView);
            tvChapterTitle = itemView.findViewById(R.id.tvChapterTitle);
            tvChapterSub = itemView.findViewById(R.id.tvChapterSub);
            btnDownload = itemView.findViewById(R.id.btnDownload);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onChapterClick(chapterList.get(pos));
                }
            });
            btnDownload.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && downloadListener != null) {
                    Chapter chapter = chapterList.get(pos);
                    downloadListener.onDownloadClick(chapter, stateOf(chapter));
                }
            });
        }

        void bind(Chapter chapter) {
            String titleStr = "Chương " + chapter.getChapterNumber() + ": " + chapter.getTitle();
            tvChapterTitle.setText(titleStr);
            String subStr = chapter.getUpdatedDate() == null
                    ? "" : "Cập nhật: " + chapter.getUpdatedDate();
            tvChapterSub.setText(subStr);

            DownloadState state = stateOf(chapter);
            btnDownload.setVisibility(downloadListener == null ? View.GONE : View.VISIBLE);
            btnDownload.setAlpha(downloadEnabled ? 1f : 0.4f);
            if (state == DownloadState.DONE) {
                btnDownload.setImageResource(R.drawable.ic_download_done);
                btnDownload.setContentDescription("Đã tải, bấm để xoá");
            } else if (state == DownloadState.QUEUED) {
                btnDownload.setImageResource(R.drawable.ic_downloading);
                btnDownload.setContentDescription("Đang tải");
            } else {
                btnDownload.setImageResource(R.drawable.ic_download);
                btnDownload.setContentDescription("Tải chương");
            }
        }
    }
}
