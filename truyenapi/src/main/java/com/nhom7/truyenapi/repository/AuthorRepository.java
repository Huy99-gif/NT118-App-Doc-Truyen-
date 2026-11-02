package com.nhom7.truyenapi.repository;

import com.nhom7.truyenapi.entity.Author;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthorRepository extends JpaRepository<Author, Long> {
}
