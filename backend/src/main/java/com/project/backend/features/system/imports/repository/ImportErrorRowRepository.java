package com.project.backend.features.system.imports.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.system.imports.entity.ImportErrorRow;

public interface ImportErrorRowRepository
        extends JpaRepository<ImportErrorRow, Long> {

    List<ImportErrorRow> findByHistoryIdOrderByRowNoAsc(Long historyId);
}