package com.nhom7.truyenapi.repository;

import com.nhom7.truyenapi.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
}
