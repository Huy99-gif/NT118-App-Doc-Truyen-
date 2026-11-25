package com.example.android_app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.android_app.R;

import java.util.ArrayList;
import java.util.List;

/** Danh sách trong tab "Đã tải": dùng cho cả cấp truyện (có ảnh bìa) và cấp chương. */
public class DownloadedStoryAdapter extends RecyclerView.Adapter<DownloadedStoryAdapter.RowViewHolder> {

    public static class Row {
        public final String title;
        public final String subtitle;
        public final String coverUrl;
        public final boolean showCover;
        public final Object payload;

        public Row(String title, String subtitle, String coverUrl, boolean showCover, Object payload) {
            this.title = title;
            this.subtitle = subtitle;
            this.coverUrl = coverUrl;
            this.showCover = showCover;
            this.payload = payload;
        }
    }

    public interface Listener {
        void onRowClick(Row row);
        void onRowLongClick(Row row);
    }

    private final List<Row> rows = new ArrayList<>();
    private final Listener listener;

    public DownloadedStoryAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setRows(List<Row> newRows) {
        rows.clear();
        if (newRows != null) rows.addAll(newRows);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RowViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_downloaded_story, parent, false);
        return new RowViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RowViewHolder holder, int position) {
        holder.bind(rows.get(position));
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    class RowViewHolder extends RecyclerView.ViewHolder {
        private final ImageView cover;
        private final TextView title;
        private final TextView subtitle;

        RowViewHolder(@NonNull View itemView) {
            super(itemView);
            cover = itemView.findViewById(R.id.ivDownloadedCover);
            title = itemView.findViewById(R.id.tvDownloadedTitle);
            subtitle = itemView.findViewById(R.id.tvDownloadedSubtitle);
            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) listener.onRowClick(rows.get(pos));
            });
            itemView.setOnLongClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) listener.onRowLongClick(rows.get(pos));
                return true;
            });
        }

        void bind(Row row) {
            title.setText(row.title);
            subtitle.setText(row.subtitle);
            cover.setVisibility(row.showCover ? View.VISIBLE : View.GONE);
            if (row.showCover) {
                // offline thì Glide dùng ảnh trong cache nếu còn, không có thì hiện placeholder
                Glide.with(cover)
                        .load(row.coverUrl)
                        .apply(new RequestOptions()
                                .placeholder(R.drawable.ic_launcher_foreground)
                                .error(R.drawable.ic_launcher_foreground))
                        .into(cover);
            }
        }
    }
}
