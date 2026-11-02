package com.nhom7.truyenapi.repository;

import com.nhom7.truyenapi.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}
