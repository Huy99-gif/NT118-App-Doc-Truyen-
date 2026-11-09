package com.example.android_app.adapter;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import com.example.android_app.R;
import com.example.android_app.model.Story;

import java.util.ArrayList;
import java.util.List;

public class StoryAdapter extends RecyclerView.Adapter<StoryAdapter.StoryViewHolder> {

    private final List<Story> stories = new ArrayList<>();
    private final OnStoryClickListener listener;

    public interface OnStoryClickListener {
        void onStoryClick(Story story);
    }

    public StoryAdapter(OnStoryClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public StoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_story, parent, false);
        return new StoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StoryViewHolder holder, int position) {
        Story story = stories.get(position);
        holder.bind(story);
    }

    @Override
    public int getItemCount() {
        return stories.size();
    }

    public void setStories(List<Story> stories) {
        this.stories.clear();
        if (stories != null) {
            this.stories.addAll(stories);
        }
        notifyDataSetChanged();
    }

    public Story getStory(int position) {
        return stories.get(position);
    }

    class StoryViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivCover;
        private final TextView tvName;
        private final TextView tvAuthor;
        private final TextView tvLatest;
        private final TextView tvProgress;
        private final TextView tvChapterCount;

        StoryViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCover = itemView.findViewById(R.id.ivStoryCover);
            tvName = itemView.findViewById(R.id.tvStoryName);
            tvAuthor = itemView.findViewById(R.id.tvAuthor);
            tvLatest = itemView.findViewById(R.id.tvLatestChapter);
            tvProgress = itemView.findViewById(R.id.tvReadProgress);
            tvChapterCount = itemView.findViewById(R.id.tvChapterCount);
            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onStoryClick(stories.get(pos));
                }
            });
        }

        void bind(Story story) {
            tvName.setText(story.getName());
            tvAuthor.setText(story.getAuthor());
            tvLatest.setText(story.getLatestChapter());
            tvChapterCount.setText("Chương " + story.getChapterCount());
            if (story.isReading()) {
                tvProgress.setText(R.string.recently_reading);
                tvProgress.setVisibility(View.VISIBLE);
            } else {
                tvProgress.setVisibility(View.GONE);
            }
            Glide.with(ivCover.getContext())
                    .load(story.getCoverUrl())
                    .apply(new RequestOptions().placeholder(R.drawable.ic_launcher_foreground).error(R.drawable.ic_launcher_foreground))
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(ivCover);
        }
    }
}
