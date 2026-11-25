package com.nhom7.truyenapi.repository;

import com.nhom7.truyenapi.entity.Rating;
import com.nhom7.truyenapi.dto.RatingSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Long> {
    Optional<Rating> findByStoryIdAndUserId(Long storyId, Long userId);

    @Query("""
            select new com.nhom7.truyenapi.dto.RatingSummary(avg(r.stars), count(r),
                max(case when r.user.id = :userId then r.stars else null end))
            from Rating r where r.story.id = :storyId
            """)
    RatingSummary summary(Long storyId, Long userId);
}
