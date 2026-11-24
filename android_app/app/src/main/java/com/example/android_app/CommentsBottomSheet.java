package com.example.android_app;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android_app.adapter.CommentAdapter;
import com.example.android_app.databinding.BottomSheetCommentsBinding;
import com.example.android_app.model.CommentResponse;
import com.example.android_app.model.CreateCommentRequest;
import com.example.android_app.model.PageResponse;
import com.example.android_app.network.ApiClient;
import com.example.android_app.util.ApiErrorParser;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CommentsBottomSheet extends BottomSheetDialogFragment {
    public static final String TAG = "CommentsBottomSheet";
    private static final String ARG_CHAPTER_ID = "chapter_id";
    private static final String ARG_CHAPTER_NUMBER = "chapter_number";
    private static final int PAGE_SIZE = 20;
    private static final float SHEET_HEIGHT_RATIO = 0.75f;

    private BottomSheetCommentsBinding binding;
    private CommentAdapter adapter;
    private long chapterId;
    private int currentPage = -1;
    private int totalPages = 1;
    private boolean loadingPage;
    private Call<PageResponse<CommentResponse>> listCall;
    private Call<CommentResponse> createCall;
    private Call<Void> deleteCall;

    public static CommentsBottomSheet newInstance(long chapterId, int chapterNumber) {
        Bundle args = new Bundle();
        args.putLong(ARG_CHAPTER_ID, chapterId);
        args.putInt(ARG_CHAPTER_NUMBER, chapterNumber);
        CommentsBottomSheet sheet = new CommentsBottomSheet();
        sheet.setArguments(args);
        return sheet;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        // đẩy hàng nhập lên trên bàn phím
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = BottomSheetCommentsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = requireArguments();
        chapterId = args.getLong(ARG_CHAPTER_ID);
        binding.tvCommentsTitle.setText("Bình luận · Chương " + args.getInt(ARG_CHAPTER_NUMBER, 1));

        adapter = new CommentAdapter(this::confirmDelete);
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        binding.rvComments.setLayoutManager(layoutManager);
        binding.rvComments.setAdapter(adapter);
        binding.rvComments.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                // gần cuối danh sách thì tải trang kế
                if (dy > 0 && !loadingPage && currentPage + 1 < totalPages
                        && layoutManager.findLastVisibleItemPosition() >= adapter.getItemCount() - 3) {
                    loadPage(currentPage + 1);
                }
            }
        });
        binding.btnSendComment.setOnClickListener(v -> sendComment());
        loadPage(0);
    }

    @Override
    public void onStart() {
        super.onStart();
        BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
        if (dialog == null) return;
        View sheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (sheet != null) {
            ViewGroup.LayoutParams params = sheet.getLayoutParams();
            params.height = (int) (getResources().getDisplayMetrics().heightPixels * SHEET_HEIGHT_RATIO);
            sheet.setLayoutParams(params);
        }
        BottomSheetBehavior<?> behavior = dialog.getBehavior();
        behavior.setSkipCollapsed(true);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    @Override
    public void onDestroyView() {
        cancel(listCall);
        cancel(createCall);
        cancel(deleteCall);
        binding = null;
        super.onDestroyView();
    }

    private void loadPage(int page) {
        String authorization = requireAuthorization();
        if (authorization == null) return;
        loadingPage = true;
        if (page == 0) binding.progressComments.setVisibility(View.VISIBLE);
        listCall = ApiClient.api().getComments(authorization, chapterId, page, PAGE_SIZE);
        listCall.enqueue(new Callback<PageResponse<CommentResponse>>() {
            @Override
            public void onResponse(Call<PageResponse<CommentResponse>> call,
                                   Response<PageResponse<CommentResponse>> response) {
                if (!isAdded() || binding == null || call.isCanceled()) return;
                loadingPage = false;
                binding.progressComments.setVisibility(View.GONE);
                if (!response.isSuccessful() || response.body() == null) {
                    toast(ApiErrorParser.getMessage(response, "Không tải được bình luận."));
                    return;
                }
                PageResponse<CommentResponse> body = response.body();
                currentPage = body.page;
                totalPages = body.totalPages;
                if (page == 0) adapter.setItems(body.items);
                else adapter.addItems(body.items);
                updateEmptyState();
            }

            @Override
            public void onFailure(Call<PageResponse<CommentResponse>> call, Throwable throwable) {
                if (!isAdded() || binding == null || call.isCanceled()) return;
                loadingPage = false;
                binding.progressComments.setVisibility(View.GONE);
                toast(getString(R.string.error_server_unreachable));
            }
        });
    }

    private void sendComment() {
        String content = binding.etComment.getText() == null
                ? "" : binding.etComment.getText().toString().trim();
        if (content.isEmpty()) return;
        String authorization = requireAuthorization();
        if (authorization == null) return;

        // khoá nút khi đang gửi để bấm 2 lần không tạo 2 bình luận
        binding.btnSendComment.setEnabled(false);
        createCall = ApiClient.api().createComment(authorization, chapterId,
                new CreateCommentRequest(content));
        createCall.enqueue(new Callback<CommentResponse>() {
            @Override
            public void onResponse(Call<CommentResponse> call, Response<CommentResponse> response) {
                if (!isAdded() || binding == null || call.isCanceled()) return;
                binding.btnSendComment.setEnabled(true);
                if (!response.isSuccessful() || response.body() == null) {
                    toast(ApiErrorParser.getMessage(response, "Không gửi được bình luận."));
                    return;
                }
                adapter.prepend(response.body());
                binding.etComment.setText("");
                binding.rvComments.scrollToPosition(0);
                updateEmptyState();
            }

            @Override
            public void onFailure(Call<CommentResponse> call, Throwable throwable) {
                if (!isAdded() || binding == null || call.isCanceled()) return;
                binding.btnSendComment.setEnabled(true);
                toast(getString(R.string.error_server_unreachable));
            }
        });
    }

    private void confirmDelete(CommentResponse comment) {
        if (comment.id == null) return;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Xoá bình luận này?")
                .setNegativeButton("Huỷ", null)
                .setPositiveButton("Xoá", (dialog, which) -> deleteComment(comment.id))
                .show();
    }

    private void deleteComment(long commentId) {
        String authorization = requireAuthorization();
        if (authorization == null) return;
        deleteCall = ApiClient.api().deleteComment(authorization, commentId);
        deleteCall.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (!isAdded() || binding == null || call.isCanceled()) return;
                if (!response.isSuccessful()) {
                    toast(ApiErrorParser.getMessage(response, "Không xoá được bình luận."));
                    return;
                }
                adapter.removeById(commentId);
                updateEmptyState();
                toast("Đã xoá bình luận.");
            }

            @Override
            public void onFailure(Call<Void> call, Throwable throwable) {
                if (!isAdded() || binding == null || call.isCanceled()) return;
                toast(getString(R.string.error_server_unreachable));
            }
        });
    }

    private void updateEmptyState() {
        binding.tvEmptyComments.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    private String requireAuthorization() {
        String authorization = ApiClient.authorization(requireContext());
        if (authorization == null) {
            toast("Vui lòng đăng nhập để sử dụng chức năng này.");
            dismiss();
        }
        return authorization;
    }

    private void toast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
    }

    private void cancel(Call<?> call) {
        if (call != null) call.cancel();
    }
}
