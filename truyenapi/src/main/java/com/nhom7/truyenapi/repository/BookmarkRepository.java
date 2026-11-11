package com.nhom7.truyenapi.repository;

import com.nhom7.truyenapi.entity.Bookmark;
import com.nhom7.truyenapi.dto.BookmarkResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    long countByUser_Id(Long userId);

    @Query(
            value = """
                    select new com.nhom7.truyenapi.dto.BookmarkResponse(
                        b.id, s.id, s.title, s.coverUrl, c.id, c.chapterNo, c.title,
                        b.charOffset, b.note)
                    from Bookmark b
                    join b.chapter c
                    join c.story s
                    where b.user.id = :userId
                    order by b.id desc
                    """,
            countQuery = "select count(b.id) from Bookmark b where b.user.id = :userId")
    Page<BookmarkResponse> findItemsByUserId(
            @Param("userId") Long userId,
            Pageable pageable);

    @Query("""
            select new com.nhom7.truyenapi.dto.BookmarkResponse(
                b.id, s.id, s.title, s.coverUrl, c.id, c.chapterNo, c.title,
                b.charOffset, b.note)
            from Bookmark b
            join b.chapter c
            join c.story s
            where b.id = :bookmarkId and b.user.id = :userId
            """)
    java.util.Optional<BookmarkResponse> findItemByIdAndUserId(
            @Param("bookmarkId") Long bookmarkId,
            @Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("delete from Bookmark b where b.id = :bookmarkId and b.user.id = :userId")
    int deleteByIdAndUserId(
            @Param("bookmarkId") Long bookmarkId,
            @Param("userId") Long userId);
}
