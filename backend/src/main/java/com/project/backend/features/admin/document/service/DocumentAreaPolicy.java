package com.project.backend.features.admin.document.service;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.project.backend.features.admin.document.dto.DocumentAreaResponse;
import com.project.backend.features.admin.document.enums.DocumentArea;
import com.project.backend.features.admin.document.enums.DocumentOperation;
import com.project.backend.features.admin.document.exception.DocumentOperationNotAllowedException;

@Component
public class DocumentAreaPolicy {

    private static final Set<DocumentOperation> READ_ONLY_OPERATIONS =
            Set.copyOf(EnumSet.of(
                    DocumentOperation.READ,
                    DocumentOperation.SEARCH,
                    DocumentOperation.DETAILS,
                    DocumentOperation.DOWNLOAD
            ));

    private static final Set<DocumentOperation> ALL_OPERATIONS =
            Set.copyOf(EnumSet.allOf(DocumentOperation.class));

    private final Map<DocumentArea, Set<DocumentOperation>> operationsByArea;

    public DocumentAreaPolicy() {
        operationsByArea = new EnumMap<>(DocumentArea.class);
        operationsByArea.put(DocumentArea.GENERAL, ALL_OPERATIONS);
        operationsByArea.put(DocumentArea.GENERATED_REPORTS, READ_ONLY_OPERATIONS);
        operationsByArea.put(DocumentArea.BACKUPS, READ_ONLY_OPERATIONS);
        operationsByArea.put(DocumentArea.TEMPLATES, READ_ONLY_OPERATIONS);
    }

    public List<DocumentAreaResponse> findAreas() {
        return List.of(DocumentArea.values())
                .stream()
                .map(area -> new DocumentAreaResponse(
                        area,
                        area.getDisplayName(),
                        allowedOperations(area)
                ))
                .toList();
    }

    public Set<DocumentOperation> allowedOperations(DocumentArea area) {
        if (area == null) {
            throw new IllegalArgumentException("documentArea は必須です。");
        }

        return operationsByArea.get(area);
    }

    public boolean isAllowed(
            DocumentArea area,
            DocumentOperation operation
    ) {
        if (operation == null) {
            return false;
        }

        return allowedOperations(area).contains(operation);
    }

    public void requireAllowed(
            DocumentArea area,
            DocumentOperation operation
    ) {
        if (!isAllowed(area, operation)) {
            throw new DocumentOperationNotAllowedException(
                    "この書類領域では操作できません。 area="
                            + area
                            + ", operation="
                            + operation
            );
        }
    }
}
