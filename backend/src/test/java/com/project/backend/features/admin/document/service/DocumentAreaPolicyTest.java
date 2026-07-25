package com.project.backend.features.admin.document.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.project.backend.features.admin.document.enums.DocumentArea;
import com.project.backend.features.admin.document.enums.DocumentOperation;
import com.project.backend.features.admin.document.exception.DocumentOperationNotAllowedException;

class DocumentAreaPolicyTest {

    private final DocumentAreaPolicy policy = new DocumentAreaPolicy();

    @Test
    void general_shouldAllowAllOperations() {
        assertThat(policy.allowedOperations(DocumentArea.GENERAL))
                .containsExactlyInAnyOrder(DocumentOperation.values());
    }

    @Test
    void managedAreas_shouldBeReadOnly() {
        assertThat(policy.allowedOperations(DocumentArea.GENERATED_REPORTS))
                .containsExactlyInAnyOrder(
                        DocumentOperation.READ,
                        DocumentOperation.SEARCH,
                        DocumentOperation.DETAILS,
                        DocumentOperation.DOWNLOAD
                );
        assertThat(policy.allowedOperations(DocumentArea.BACKUPS))
                .isEqualTo(policy.allowedOperations(DocumentArea.GENERATED_REPORTS));
        assertThat(policy.allowedOperations(DocumentArea.TEMPLATES))
                .isEqualTo(policy.allowedOperations(DocumentArea.GENERATED_REPORTS));
    }

    @Test
    void requireAllowed_shouldRejectDeleteForBackupArea() {
        assertThatThrownBy(() ->
                policy.requireAllowed(
                        DocumentArea.BACKUPS,
                        DocumentOperation.DELETE
                ))
                .isInstanceOf(DocumentOperationNotAllowedException.class)
                .hasMessageContaining("BACKUPS")
                .hasMessageContaining("DELETE");
    }
}
