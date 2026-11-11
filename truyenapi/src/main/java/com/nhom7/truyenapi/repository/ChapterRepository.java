package com.nhom7.truyenapi.repository;

import com.nhom7.truyenapi.entity.Chapter;
import com.nhom7.truyenapi.dto.ChapterDetailResponse;
import com.nhom7.truyenapi.dto.ChapterSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ChapterRepository extends JpaRepository<Chapter, Long> {
    @Query(
            value = """
                    select new com.nhom7.truyenapi.dto.ChapterSummaryResponse(
                        c.id, c.chapterNo, c.title, c.updatedAt)
                    from Chapter c
                    where c.story.id = :storyId
                    order by c.chapterNo asc, c.id asc
                    """,
            countQuery = "select count(c.id) from Chapter c where c.story.id = :storyId")
    Page<ChapterSummaryResponse> findSummariesByStoryId(
            @Param("storyId") Long storyId,
            Pageable pageable);

    @Query("""
            select new com.nhom7.truyenapi.dto.ChapterDetailResponse(
                c.id, c.story.id, c.chapterNo, c.title, c.content, c.updatedAt)
            from Chapter c
            where c.id = :id
            """)
    Optional<ChapterDetailResponse> findDetailById(@Param("id") Long id);

    Optional<Chapter> findByIdAndStory_Id(Long id, Long storyId);

    @Query("""
            select new com.nhom7.truyenapi.dto.ChapterDetailResponse(
                c.id, c.story.id, c.chapterNo, c.title, c.content, c.updatedAt)
            from Chapter c
            where c.id in :ids
            """)
    List<ChapterDetailResponse> findDetailsByIdIn(@Param("ids") Collection<Long> ids);
}
