package com.nhom7.truyenapi.repository;

import com.nhom7.truyenapi.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RatingRepository extends JpaRepository<Rating, Long> {
}
