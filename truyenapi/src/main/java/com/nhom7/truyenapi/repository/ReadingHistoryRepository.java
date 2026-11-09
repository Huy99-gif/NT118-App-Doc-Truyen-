package com.nhom7.truyenapi.repository;

import com.nhom7.truyenapi.entity.ReadingHistory;
import com.nhom7.truyenapi.entity.ReadingHistoryId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReadingHistoryRepository extends JpaRepository<ReadingHistory, ReadingHistoryId> {
}
