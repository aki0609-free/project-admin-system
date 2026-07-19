package com.project.backend.features.system.backup.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.system.backup.dto.BackupTargetSummary;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BackupTargetService {

    private final BackupDefinitionService backupDefinitionService;

    @Transactional(readOnly = true)
    public List<BackupTargetSummary> findBackupTargets() {
        return backupDefinitionService.findBackupEnabledTargets();
    }
}