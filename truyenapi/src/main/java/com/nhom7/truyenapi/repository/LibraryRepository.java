package com.nhom7.truyenapi.repository;

import com.nhom7.truyenapi.entity.Library;
import com.nhom7.truyenapi.entity.LibraryId;
import com.nhom7.truyenapi.dto.LibraryItemResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface LibraryRepository extends JpaRepository<Library, LibraryId> {
    long countById_UserId(Long userId);

    @Query(
            value = """
                    select new com.nhom7.truyenapi.dto.LibraryItemResponse(
                        s.id, s.title, a.name, s.coverUrl, s.status, s.featured,
                        s.updatedAt, l.addedAt)
                    from Library l
                    join l.story s
                    join s.author a
                    where l.id.userId = :userId
                    order by l.addedAt desc, s.id asc
                    """,
            countQuery = "select count(l.id.storyId) from Library l where l.id.userId = :userId")
    Page<LibraryItemResponse> findItemsByUserId(
            @Param("userId") Long userId,
            Pageable pageable);

    @Query("""
            select new com.nhom7.truyenapi.dto.LibraryItemResponse(
                s.id, s.title, a.name, s.coverUrl, s.status, s.featured,
                s.updatedAt, l.addedAt)
            from Library l
            join l.story s
            join s.author a
            where l.id.userId = :userId and l.id.storyId = :storyId
            """)
    java.util.Optional<LibraryItemResponse> findItemByUserIdAndStoryId(
            @Param("userId") Long userId,
            @Param("storyId") Long storyId);

    @Modifying
    @Transactional
    @Query("delete from Library l where l.id.userId = :userId and l.id.storyId = :storyId")
    int deleteByUserIdAndStoryId(
            @Param("userId") Long userId,
            @Param("storyId") Long storyId);
}
