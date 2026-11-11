package com.nhom7.truyenapi.repository;

import com.nhom7.truyenapi.entity.ReadingHistory;
import com.nhom7.truyenapi.entity.ReadingHistoryId;
import com.nhom7.truyenapi.dto.ReadingHistoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReadingHistoryRepository extends JpaRepository<ReadingHistory, ReadingHistoryId> {
    @Query(
            value = """
                    select new com.nhom7.truyenapi.dto.ReadingHistoryResponse(
                        s.id, s.title, s.coverUrl, c.id, c.chapterNo, c.title,
                        h.charOffset, h.updatedAt)
                    from ReadingHistory h
                    join h.story s
                    left join h.chapter c
                    where h.id.userId = :userId
                    order by h.updatedAt desc, s.id asc
                    """,
            countQuery = "select count(h.id.storyId) from ReadingHistory h where h.id.userId = :userId")
    Page<ReadingHistoryResponse> findItemsByUserId(
            @Param("userId") Long userId,
            Pageable pageable);

    long countById_UserId(Long userId);
}
