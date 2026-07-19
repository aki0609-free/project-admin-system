package com.project.backend.features.system.backup.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.system.backup.entity.BackupHistory;

public interface BackupHistoryRepository extends JpaRepository<BackupHistory, Long> {

    List<BackupHistory> findByDeletedAtIsNullOrderByExecutedAtDesc();
}