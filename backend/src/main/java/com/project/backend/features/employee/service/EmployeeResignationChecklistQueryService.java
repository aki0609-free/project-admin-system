package com.project.backend.features.employee.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.employee.dto.EmployeeResignationChecklistResponse;
import com.project.backend.features.employee.entity.EmployeeResignationChecklistMaster;
import com.project.backend.features.employee.repository.EmployeeResignationChecklistRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeResignationChecklistQueryService {

    private final EmployeeResignationChecklistRepository repository;

    public List<EmployeeResignationChecklistResponse> findAllActive() {
        return repository
                .findAllByActiveFlagTrueAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private EmployeeResignationChecklistResponse toResponse(
            EmployeeResignationChecklistMaster entity
    ) {
        return EmployeeResignationChecklistResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .requiredFlag(entity.isRequiredFlag())
                .displayOrder(entity.getDisplayOrder())
                .build();
    }
}