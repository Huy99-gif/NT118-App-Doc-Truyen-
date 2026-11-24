package com.nhom7.truyenapi.repository;

import com.nhom7.truyenapi.dto.CommentResponse;
import com.nhom7.truyenapi.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    // join user luôn trong 1 query để lấy displayName, tránh n+1
    @Query(
            value = """
                    select new com.nhom7.truyenapi.dto.CommentResponse(
                        c.id, u.id, u.displayName, c.content, c.createdAt,
                        case when u.id = :currentUserId then true else false end)
                    from Comment c
                    join c.user u
                    where c.chapter.id = :chapterId
                    order by c.id desc
                    """,
            countQuery = "select count(c.id) from Comment c where c.chapter.id = :chapterId")
    Page<CommentResponse> findItemsByChapterId(
            @Param("chapterId") Long chapterId,
            @Param("currentUserId") Long currentUserId,
            Pageable pageable);

    @Query("""
            select new com.nhom7.truyenapi.dto.CommentResponse(
                c.id, u.id, u.displayName, c.content, c.createdAt,
                case when u.id = :currentUserId then true else false end)
            from Comment c
            join c.user u
            where c.id = :commentId
            """)
    Optional<CommentResponse> findItemById(
            @Param("commentId") Long commentId,
            @Param("currentUserId") Long currentUserId);
}
