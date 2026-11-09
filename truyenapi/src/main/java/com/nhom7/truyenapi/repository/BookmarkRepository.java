package com.nhom7.truyenapi.repository;

import com.nhom7.truyenapi.entity.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
}
