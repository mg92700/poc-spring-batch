package com.poc_spring_batch.repository;


import com.poc_spring_batch.domain.FileRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileRecordRepository extends JpaRepository<FileRecord, Long> {

    List<FileRecord> findByStatus(FileRecord.FileStatus status);

    @Query("SELECT f FROM FileRecord f ORDER BY f.readAt DESC")
    List<FileRecord> findAllOrderByReadAtDesc();
}
