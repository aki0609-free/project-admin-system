package com.project.backend.features.system.mail.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.project.backend.features.customer.entity.CustomerEmployee;
import com.project.backend.features.customer.repository.CustomerEmployeeRepository;
import com.project.backend.features.system.mail.entity.MailRecipient;
import com.project.backend.features.system.mail.entity.MailRecipientGroup;
import com.project.backend.features.system.mail.enums.MailRecipientType;
import com.project.backend.features.system.mail.repository.MailRecipientGroupRepository;

class CustomerInvoiceMailGroupSyncServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-31T00:00:00Z");

    private final MailRecipientGroupRepository groupRepository =
            mock(MailRecipientGroupRepository.class);
    private final CustomerEmployeeRepository employeeRepository =
            mock(CustomerEmployeeRepository.class);
    private final CustomerInvoiceMailGroupSyncService service =
            new CustomerInvoiceMailGroupSyncService(
                    groupRepository,
                    employeeRepository,
                    Clock.fixed(NOW, ZoneOffset.UTC)
            );

    @Test
    void synchronize_shouldCreateCustomerGroupAndReflectToCcAndInactiveRecipients() {
        when(groupRepository.findByGroupKeyIncludingDeleted("CUSTOMER_INVOICE_10"))
                .thenReturn(Optional.empty());
        when(employeeRepository.findByCustomerIdAndDeletedAtIsNullOrderByIdAsc(10L))
                .thenReturn(List.of(
                        employee(1L, "To担当", "to@example.com", true, false),
                        employee(2L, "Cc担当", "cc@example.com", false, true),
                        employee(3L, "未指定担当", "inactive@example.com", false, false),
                        employee(4L, "メールなし", null, false, false)
                ));

        service.synchronize(10L, "株式会社テスト");

        ArgumentCaptor<MailRecipientGroup> captor =
                ArgumentCaptor.forClass(MailRecipientGroup.class);
        verify(groupRepository).save(captor.capture());
        MailRecipientGroup saved = captor.getValue();

        assertThat(saved.getGroupKey()).isEqualTo("CUSTOMER_INVOICE_10");
        assertThat(saved.getGroupName()).isEqualTo("請求書送付先：株式会社テスト");
        assertThat(saved.getRecipients()).hasSize(3);
        assertThat(saved.getRecipients())
                .extracting(MailRecipient::getRecipientType)
                .containsExactly(MailRecipientType.TO, MailRecipientType.CC, MailRecipientType.TO);
        assertThat(saved.getRecipients())
                .extracting(MailRecipient::isActiveFlag)
                .containsExactly(true, true, false);
    }

    @Test
    void synchronize_shouldKeepManualRecipientsAndReplaceManagedRecipients() {
        MailRecipientGroup existing = new MailRecipientGroup();
        existing.setGroupKey("CUSTOMER_INVOICE_10");

        MailRecipient manual = new MailRecipient();
        manual.setRecipientKey("MANUAL_ACCOUNTING");
        manual.setEmail("manual@example.com");
        manual.setGroup(existing);
        existing.getRecipients().add(manual);

        MailRecipient staleManaged = new MailRecipient();
        staleManaged.setRecipientKey("CUSTOMER_EMPLOYEE_99");
        staleManaged.setEmail("old@example.com");
        staleManaged.setGroup(existing);
        existing.getRecipients().add(staleManaged);

        when(groupRepository.findByGroupKeyIncludingDeleted("CUSTOMER_INVOICE_10"))
                .thenReturn(Optional.of(existing));
        when(employeeRepository.findByCustomerIdAndDeletedAtIsNullOrderByIdAsc(10L))
                .thenReturn(List.of(employee(1L, "新担当", "new@example.com", true, false)));

        service.synchronize(10L, "株式会社テスト");

        assertThat(existing.getRecipients())
                .extracting(MailRecipient::getRecipientKey)
                .containsExactly("MANUAL_ACCOUNTING", "CUSTOMER_EMPLOYEE_1");
        verify(groupRepository).save(existing);
    }

    @Test
    void delete_shouldSoftDeleteGroupAndRecipients() {
        MailRecipientGroup group = new MailRecipientGroup();
        group.setActiveFlag(true);
        MailRecipient recipient = new MailRecipient();
        recipient.setActiveFlag(true);
        group.getRecipients().add(recipient);
        when(groupRepository.findByGroupKeyIncludingDeleted("CUSTOMER_INVOICE_10"))
                .thenReturn(Optional.of(group));

        service.delete(10L);

        assertThat(group.isActiveFlag()).isFalse();
        assertThat(group.getDeletedAt()).isEqualTo(NOW);
        assertThat(recipient.isActiveFlag()).isFalse();
        assertThat(recipient.getDeletedAt()).isEqualTo(NOW);
    }

    private CustomerEmployee employee(
            Long id,
            String name,
            String email,
            boolean to,
            boolean cc
    ) {
        CustomerEmployee employee = new CustomerEmployee();
        employee.setId(id);
        employee.setName(name);
        employee.setEmail(email);
        employee.setInvoiceToFlag(to);
        employee.setInvoiceCcFlag(cc);
        return employee;
    }
}
