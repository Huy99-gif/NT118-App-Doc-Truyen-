package com.nhom7.truyenapi.repository;

import com.nhom7.truyenapi.entity.Story;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoryRepository extends JpaRepository<Story, Long> {
}
