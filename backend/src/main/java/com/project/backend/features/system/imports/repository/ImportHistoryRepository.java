package com.project.backend.features.system.imports.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.system.imports.entity.ImportHistory;

public interface ImportHistoryRepository extends JpaRepository<ImportHistory, Long> {

    List<ImportHistory> findByDeletedAtIsNullOrderByExecutedAtDesc();
}