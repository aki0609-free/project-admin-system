package com.project.backend.features.system.batch.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.system.batch.entity.BatchExecutionLog;

public interface BatchExecutionLogRepository extends JpaRepository<BatchExecutionLog, Long> {

    List<BatchExecutionLog> findAllByDeletedAtIsNullOrderByIdDesc();

    List<BatchExecutionLog> findByJobCodeAndDeletedAtIsNullOrderByIdDesc(String jobCode);
}