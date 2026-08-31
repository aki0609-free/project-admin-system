package com.project.backend.features.system.mail.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import com.project.backend.features.customer.entity.CustomerEmployee;
import com.project.backend.features.customer.repository.CustomerEmployeeRepository;
import com.project.backend.features.customer.service.integration.CustomerInvoiceMailGroupSynchronizer;
import com.project.backend.features.system.mail.entity.MailRecipient;
import com.project.backend.features.system.mail.entity.MailRecipientGroup;
import com.project.backend.features.system.mail.enums.MailRecipientType;
import com.project.backend.features.system.mail.repository.MailRecipientGroupRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerInvoiceMailGroupSyncService
        implements CustomerInvoiceMailGroupSynchronizer {

    static final String GROUP_KEY_PREFIX = "CUSTOMER_INVOICE_";
    static final String RECIPIENT_KEY_PREFIX = "CUSTOMER_EMPLOYEE_";

    private final MailRecipientGroupRepository groupRepository;
    private final CustomerEmployeeRepository customerEmployeeRepository;
    private final Clock clock;

    @Override
    public void synchronize(Long customerId, String customerName) {
        String groupKey = groupKey(customerId);
        MailRecipientGroup group = groupRepository.findByGroupKeyIncludingDeleted(groupKey)
                .orElseGet(MailRecipientGroup::new);

        group.setGroupKey(groupKey);
        group.setGroupName("請求書送付先：" + customerName);
        group.setActiveFlag(true);
        group.setDeletedAt(null);

        // 管理画面から追加された宛先は保持し、顧客担当者由来の宛先だけを再同期する。
        group.getRecipients().removeIf(this::isManagedRecipient);

        List<CustomerEmployee> employees = customerEmployeeRepository
                .findByCustomerIdAndDeletedAtIsNullOrderByIdAsc(customerId);
        for (CustomerEmployee employee : employees) {
            if (employee.getEmail() == null || employee.getEmail().isBlank()) {
                continue;
            }

            MailRecipient recipient = new MailRecipient();
            recipient.setGroup(group);
            recipient.setRecipientKey(RECIPIENT_KEY_PREFIX + employee.getId());
            recipient.setRecipientName(employee.getName());
            recipient.setEmail(employee.getEmail().trim());
            recipient.setRecipientType(Boolean.TRUE.equals(employee.getInvoiceCcFlag())
                    ? MailRecipientType.CC
                    : MailRecipientType.TO);
            recipient.setActiveFlag(Boolean.TRUE.equals(employee.getInvoiceToFlag())
                    || Boolean.TRUE.equals(employee.getInvoiceCcFlag()));
            group.getRecipients().add(recipient);
        }

        groupRepository.save(group);
    }

    @Override
    public void delete(Long customerId) {
        groupRepository.findByGroupKeyIncludingDeleted(groupKey(customerId)).ifPresent(group -> {
            Instant deletedAt = Instant.now(clock);
            group.setActiveFlag(false);
            group.setDeletedAt(deletedAt);
            group.getRecipients().forEach(recipient -> {
                recipient.setActiveFlag(false);
                recipient.setDeletedAt(deletedAt);
            });
        });
    }

    private boolean isManagedRecipient(MailRecipient recipient) {
        return recipient.getRecipientKey() != null
                && recipient.getRecipientKey().startsWith(RECIPIENT_KEY_PREFIX);
    }

    private String groupKey(Long customerId) {
        return GROUP_KEY_PREFIX + customerId;
    }
}
