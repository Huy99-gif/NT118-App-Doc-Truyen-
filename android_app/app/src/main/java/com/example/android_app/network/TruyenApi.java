package com.example.android_app.network;

import com.example.android_app.model.AddLibraryRequest;
import com.example.android_app.model.BookmarkResponse;
import com.example.android_app.model.ChapterDetailResponse;
import com.example.android_app.model.ChapterSummaryResponse;
import com.example.android_app.model.CreateBookmarkRequest;
import com.example.android_app.model.LibraryItemResponse;
import com.example.android_app.model.PageResponse;
import com.example.android_app.model.ReadingHistoryResponse;
import com.example.android_app.model.StoryDetailResponse;
import com.example.android_app.model.StorySummaryResponse;
import com.example.android_app.model.UpdateReadingHistoryRequest;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface TruyenApi extends AuthApi {
    @GET("stories")
    Call<PageResponse<StorySummaryResponse>> getStories(
            @Header("Authorization") String authorization,
            @Query("type") String type,
            @Query("category") String category,
            @Query("page") int page,
            @Query("size") int size);

    @GET("stories/search")
    Call<PageResponse<StorySummaryResponse>> searchStories(
            @Header("Authorization") String authorization,
            @Query("q") String query,
            @Query("author") String author,
            @Query("status") String status,
            @Query("page") int page,
            @Query("size") int size);

    @GET("stories/{id}")
    Call<StoryDetailResponse> getStory(
            @Header("Authorization") String authorization,
            @Path("id") long storyId);

    @GET("stories/{id}/chapters")
    Call<PageResponse<ChapterSummaryResponse>> getStoryChapters(
            @Header("Authorization") String authorization,
            @Path("id") long storyId,
            @Query("page") int page,
            @Query("size") int size);

    @GET("chapters/{id}")
    Call<ChapterDetailResponse> getChapter(
            @Header("Authorization") String authorization,
            @Path("id") long chapterId);

    @GET("chapters/batch")
    Call<List<ChapterDetailResponse>> getChaptersBatch(
            @Header("Authorization") String authorization,
            @Query("ids") String ids);

    @GET("library")
    Call<PageResponse<LibraryItemResponse>> getLibrary(
            @Header("Authorization") String authorization,
            @Query("page") int page,
            @Query("size") int size);

    @POST("library")
    Call<Void> addToLibrary(
            @Header("Authorization") String authorization,
            @Body AddLibraryRequest request);

    @DELETE("library/{storyId}")
    Call<Void> removeFromLibrary(
            @Header("Authorization") String authorization,
            @Path("storyId") long storyId);

    @GET("history")
    Call<PageResponse<ReadingHistoryResponse>> getReadingHistory(
            @Header("Authorization") String authorization,
            @Query("page") int page,
            @Query("size") int size);

    @PUT("history")
    Call<ReadingHistoryResponse> updateReadingHistory(
            @Header("Authorization") String authorization,
            @Body UpdateReadingHistoryRequest request);

    @GET("bookmarks")
    Call<PageResponse<BookmarkResponse>> getBookmarks(
            @Header("Authorization") String authorization,
            @Query("page") int page,
            @Query("size") int size);

    @POST("bookmarks")
    Call<BookmarkResponse> createBookmark(
            @Header("Authorization") String authorization,
            @Body CreateBookmarkRequest request);

    @DELETE("bookmarks/{id}")
    Call<Void> deleteBookmark(
            @Header("Authorization") String authorization,
            @Path("id") long bookmarkId);
}
