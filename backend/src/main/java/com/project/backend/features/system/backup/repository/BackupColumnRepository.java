package com.project.backend.features.system.backup.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.system.backup.entity.BackupColumn;

public interface BackupColumnRepository extends JpaRepository<BackupColumn, Long> {
}