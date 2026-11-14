package com.example.android_app;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.android_app.adapter.StoryAdapter;
import com.example.android_app.databinding.ActivityMainBinding;
import com.example.android_app.model.PageResponse;
import com.example.android_app.model.Story;
import com.example.android_app.model.StorySummaryResponse;
import com.example.android_app.network.ApiClient;
import com.example.android_app.util.ApiErrorParser;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private StoryAdapter adapter;
    private Call<PageResponse<StorySummaryResponse>> storiesCall;
    private String selectedType = "latest";
    private String selectedCategoryId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbar();
        setupRecyclerView();
        setupSearch();
        setupStoryTabs();
        setupBottomNavigation();
        loadStories(null);
    }

    @Override
    protected void onDestroy() {
        if (storiesCall != null) {
            storiesCall.cancel();
        }
        super.onDestroy();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        binding.toolbar.setTitleTextColor(getColor(R.color.text_primary));
    }

    private void setupRecyclerView() {
        adapter = new StoryAdapter(story -> {
            // Mở màn hình chi tiết truyện với dữ liệu được truyền qua Intent
            Intent intent = new Intent(MainActivity.this, StoryDetailActivity.class);
            intent.putExtra("story_id", story.getId());
            intent.putExtra("story_name", story.getName());
            intent.putExtra("story_author", story.getAuthor());
            intent.putExtra("story_cover", story.getCoverUrl());
            intent.putExtra("story_chapter_count", story.getChapterCount());
            startActivity(intent);
        });
        binding.recyclerView.setLayoutManager(new GridLayoutManager(this, 1));
        binding.recyclerView.setAdapter(adapter);
    }

    private void setupSearch() {
        binding.etSearch.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        binding.etSearch.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_UNSPECIFIED) {
                String query = binding.etSearch.getText() == null
                        ? "" : binding.etSearch.getText().toString().trim();
                loadStories(query.isEmpty() ? null : query);
                return true;
            }
            return false;
        });

        // Bấm vào kính lúp để tìm kiếm
        binding.ivSearchIcon.setOnClickListener(v -> {
            String query = binding.etSearch.getText() == null
                    ? "" : binding.etSearch.getText().toString().trim();
            loadStories(query.isEmpty() ? null : query);
        });

        // Tự động nạp lại danh sách mặc định khi ô tìm kiếm bị xóa rỗng
        binding.etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                if (s == null || s.toString().trim().isEmpty()) {
                    loadStories(null);
                }
            }
        });

        binding.btnFilter.setOnClickListener(v -> showCategoryFilter());
    }

    private void setupStoryTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.hot));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.recent));
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                selectedType = tab.getPosition() == 0 ? "featured" : "latest";
                String query = binding.etSearch.getText() == null
                        ? "" : binding.etSearch.getText().toString().trim();
                loadStories(query.isEmpty() ? null : query);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                showHomeContent();
                return true;
            } else if (id == R.id.nav_library) {
                showFragment(new LibraryFragment());
                return true;
            } else if (id == R.id.nav_account) {
                showFragment(new ProfileFragment());
                return true;
            } else if (id == R.id.nav_categories) {
                showCategoryFilter();
                return true;
            }
            return true;
        });
        binding.bottomNavigation.setSelectedItemId(R.id.nav_home);
    }

    private void showHomeContent() {
        binding.layoutHomeContent.setVisibility(View.VISIBLE);
        binding.fragmentContainer.setVisibility(View.GONE);
    }

    private void showFragment(Fragment fragment) {
        binding.layoutHomeContent.setVisibility(View.GONE);
        binding.fragmentContainer.setVisibility(View.VISIBLE);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    public void openLibraryTab(int tabPosition) {
        if (binding.bottomNavigation.getSelectedItemId() == R.id.nav_library) {
            showFragment(new LibraryFragment());
        } else {
            binding.bottomNavigation.setSelectedItemId(R.id.nav_library);
        }
        binding.fragmentContainer.post(() -> {
            Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
            if (fragment instanceof LibraryFragment) {
                ((LibraryFragment) fragment).selectTab(tabPosition);
            }
        });
    }

    private void showCategoryFilter() {
        EditText categoryInput = new EditText(this);
        categoryInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        categoryInput.setSingleLine(true);
        categoryInput.setHint("ID thể loại");
        if (selectedCategoryId != null) categoryInput.setText(selectedCategoryId);
        new MaterialAlertDialogBuilder(this)
                .setTitle("Lọc theo thể loại")
                .setMessage("Nhập ID thể loại từ dữ liệu backend.")
                .setView(categoryInput)
                .setNegativeButton("Hủy", null)
                .setNeutralButton("Xóa lọc", (dialog, which) -> {
                    selectedCategoryId = null;
                    reloadCurrentStories();
                })
                .setPositiveButton("Áp dụng", (dialog, which) -> {
                    String category = categoryInput.getText().toString().trim();
                    selectedCategoryId = category.isEmpty() ? null : category;
                    reloadCurrentStories();
                })
                .show();
    }

    private void reloadCurrentStories() {
        String query = binding.etSearch.getText() == null
                ? "" : binding.etSearch.getText().toString().trim();
        loadStories(query.isEmpty() ? null : query);
    }

    private void loadStories(String query) {
        String authorization = ApiClient.authorization(this);
        if (authorization == null) {
            adapter.setStories(null);
            binding.progressBar.setVisibility(View.GONE);
            binding.tvEmpty.setText("Đăng nhập để xem danh sách truyện.");
            binding.tvEmpty.setVisibility(View.VISIBLE);
            return;
        }

        if (storiesCall != null) {
            storiesCall.cancel();
        }
        if (query != null && selectedCategoryId != null) {
            Toast.makeText(this, "Bộ lọc thể loại áp dụng cho danh sách, không áp dụng khi tìm kiếm.",
                    Toast.LENGTH_SHORT).show();
        }
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.tvEmpty.setVisibility(View.GONE);
        storiesCall = query == null
                ? ApiClient.api().getStories(authorization, selectedType, selectedCategoryId, 0, 50)
                : ApiClient.api().searchStories(authorization, query, null, null, 0, 50);
        storiesCall.enqueue(new Callback<PageResponse<StorySummaryResponse>>() {
            @Override
            public void onResponse(Call<PageResponse<StorySummaryResponse>> call,
                                   Response<PageResponse<StorySummaryResponse>> response) {
                if (isFinishing() || isDestroyed() || call.isCanceled()) return;
                binding.progressBar.setVisibility(View.GONE);
                if (!response.isSuccessful() || response.body() == null) {
                    binding.tvEmpty.setText(ApiErrorParser.getMessage(
                            response, getString(R.string.error_server_unreachable)));
                    binding.tvEmpty.setVisibility(View.VISIBLE);
                    return;
                }

                List<Story> stories = new java.util.ArrayList<>();
                for (StorySummaryResponse item : response.body().items) {
                    Story story = new Story(String.valueOf(item.id), item.title, item.authorName,
                            item.coverUrl, null, 0);
                    story.setStatus(item.status);
                    stories.add(story);
                }
                adapter.setStories(stories);
                binding.tvEmpty.setText(R.string.empty_truyen);
                binding.tvEmpty.setVisibility(stories.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onFailure(Call<PageResponse<StorySummaryResponse>> call, Throwable throwable) {
                if (isFinishing() || isDestroyed() || call.isCanceled()) return;
                binding.progressBar.setVisibility(View.GONE);
                binding.tvEmpty.setText(R.string.error_server_unreachable);
                binding.tvEmpty.setVisibility(View.VISIBLE);
            }
        });
    }
}
