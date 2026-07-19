package com.project.backend.features.system.backup.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.system.backup.dto.BackupTargetResponse;
import com.project.backend.features.system.backup.mapper.BackupTargetMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BackupTargetQueryService {

    private final BackupDefinitionService backupDefinitionService;
    private final BackupTargetLookupService lookupService;
    private final BackupTargetMapper mapper;

    public List<BackupTargetResponse> findAll() {
        return mapper.toResponseListFromSummaries(
                backupDefinitionService.findBackupEnabledTargets()
        );
    }

    public BackupTargetResponse findDetail(Long id) {
        return mapper.toResponse(
                lookupService.find(id)
        );
    }
}