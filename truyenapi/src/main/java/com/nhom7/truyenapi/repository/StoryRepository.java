package com.nhom7.truyenapi.repository;

import com.nhom7.truyenapi.entity.Story;
import com.nhom7.truyenapi.dto.StorySummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StoryRepository extends JpaRepository<Story, Long> {
    @EntityGraph(attributePaths = {"author", "categories"})
    @Query("select s from Story s where s.id = :id")
    Optional<Story> findDetailedById(@Param("id") Long id);

    @Query(
            value = """
                    select new com.nhom7.truyenapi.dto.StorySummaryResponse(
                        s.id, s.title, a.name, s.coverUrl, s.status, s.featured, s.updatedAt)
                    from Story s join s.author a
                    where (:featuredOnly = false or s.featured = true)
                      and (:categoryId is null or exists (
                          select c.id from Category c join c.stories categorizedStory
                          where categorizedStory.id = s.id and c.id = :categoryId))
                      and (:hasQuery = false
                          or lower(s.title) like :queryPattern escape '!'
                          or lower(s.summary) like :queryPattern escape '!')
                      and (:authorId is null or s.author.id = :authorId)
                      and (:status is null or s.status = :status)
                    """,
            countQuery = """
                    select count(s.id)
                    from Story s
                    where (:featuredOnly = false or s.featured = true)
                      and (:categoryId is null or exists (
                          select c.id from Category c join c.stories categorizedStory
                          where categorizedStory.id = s.id and c.id = :categoryId))
                      and (:hasQuery = false
                          or lower(s.title) like :queryPattern escape '!'
                          or lower(s.summary) like :queryPattern escape '!')
                      and (:authorId is null or s.author.id = :authorId)
                      and (:status is null or s.status = :status)
                    """)
    Page<StorySummaryResponse> findStories(
            @Param("featuredOnly") boolean featuredOnly,
            @Param("categoryId") Long categoryId,
            @Param("hasQuery") boolean hasQuery,
            @Param("queryPattern") String queryPattern,
            @Param("authorId") Long authorId,
            @Param("status") String status,
            Pageable pageable);
}
