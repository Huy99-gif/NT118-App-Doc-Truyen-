package com.nhom7.truyenapi.repository;

import com.nhom7.truyenapi.entity.Library;
import com.nhom7.truyenapi.entity.LibraryId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LibraryRepository extends JpaRepository<Library, LibraryId> {
}
