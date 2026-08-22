package com.project.backend.features.customer.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.customer.dto.CustomerPaymentConfirmRequest;
import com.project.backend.features.customer.dto.CustomerTransactionClosingRequest;
import com.project.backend.features.customer.dto.CustomerTransactionRequest;
import com.project.backend.features.customer.entity.CustomerTransaction;
import com.project.backend.features.customer.enums.CustomerPaymentStatus;
import com.project.backend.features.customer.mapper.CustomerTransactionMapper;
import com.project.backend.features.customer.repository.CustomerRepository;
import com.project.backend.features.customer.repository.CustomerTransactionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerTransactionCommandService {

    private final CustomerRepository customerRepository;
    private final CustomerTransactionRepository repository;
    private final CustomerTransactionMapper mapper;

    @SuppressWarnings("null")
    public Long create(Long customerId, CustomerTransactionRequest request) {
        validateCustomerExists(customerId);
        validate(request);
        validateTargetMonthDuplicate(customerId, null, request.targetMonth());

        CustomerTransaction entity = mapper.toEntity(customerId, request);
        entity.setSourceType("MANUAL");
        refreshPaymentStatus(entity);
        return repository.save(entity).getId();
    }

    public Long upsertFromMonthlyClosing(CustomerTransactionClosingRequest request) {
        validateClosingRequest(request);
        validateCustomerExists(request.customerId());

        CustomerTransaction entity = repository
                .findByCustomerIdAndTargetMonthAndDeletedAtIsNull(
                        request.customerId(),
                        request.targetMonth()
                )
                .orElseGet(CustomerTransaction::new);

        if (entity.getPaymentStatus() == CustomerPaymentStatus.PAID
                || entity.getPaymentStatus()
                        == CustomerPaymentStatus.OVERPAID) {
            throw new IllegalStateException(
                    "入金済みの取引は月次締め処理から更新できません。customerId="
                            + request.customerId()
                            + ", targetMonth="
                            + request.targetMonth());
        }

        mapper.applyFromClosing(entity, request);
        refreshPaymentStatus(entity);

        return repository.save(entity).getId();
    }

    @SuppressWarnings("null")
    public void update(
            Long customerId,
            Long transactionId,
            CustomerTransactionRequest request) {
        validateCustomerExists(customerId);
        validate(request);

        CustomerTransaction entity = findOwnedTransaction(customerId, transactionId);
        validateTargetMonthDuplicate(
                customerId,
                transactionId,
                request.targetMonth()
        );

        mapper.apply(entity, request);
        refreshPaymentStatus(entity);

        repository.save(entity);
    }

    public void confirmPayment(
            Long customerId,
            Long transactionId,
            CustomerPaymentConfirmRequest request) {
        validateCustomerExists(customerId);

        if (request == null) {
            throw new IllegalArgumentException("CustomerPaymentConfirmRequest は必須です。");
        }

        CustomerTransaction entity = findOwnedTransaction(customerId, transactionId);

        applyPaymentConfirmation(entity, request);
        repository.save(entity);
    }

    public void confirmPaymentFromLedger(
            Long customerId,
            Long transactionId,
            String targetMonth,
            CustomerPaymentConfirmRequest request
    ) {
        validateCustomerExists(customerId);
        if (targetMonth == null || targetMonth.isBlank()) {
            throw new IllegalArgumentException("targetMonth は必須です。");
        }
        CustomerTransaction entity = findOwnedTransaction(
                customerId,
                transactionId
        );
        if (!targetMonth.equals(entity.getTargetMonth())) {
            throw new IllegalArgumentException(
                    "入金確認表の対象月と取引の対象月が一致しません。id="
                            + transactionId
            );
        }
        applyPaymentConfirmation(entity, request);
        repository.save(entity);
    }

    private void applyPaymentConfirmation(
            CustomerTransaction entity,
            CustomerPaymentConfirmRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "CustomerPaymentConfirmRequest は必須です。"
            );
        }

        int adjustmentAmount = toZero(request.adjustmentAmount());
        validateNonNegative("入金額", request.paidAmount());
        validateNonNegative("手数料", request.fee());
        validateNonNegative("相殺額", request.offsetAmount());
        validateNote(request.note());
        validateAdjustmentReason(adjustmentAmount, request.note());

        entity.setConfirmedPaymentDate(request.confirmedPaymentDate());
        entity.setPaidAmount(toZero(request.paidAmount()));
        entity.setFee(toZero(request.fee()));
        entity.setOffsetAmount(toZero(request.offsetAmount()));
        entity.setAdjustmentAmount(adjustmentAmount);

        refreshPaymentStatus(entity);

        if (request.note() != null && !request.note().isBlank()) {
            entity.setNote(request.note());
        } else {
            entity.setNote(null);
        }
    }

    @SuppressWarnings("null")
    public void delete(Long customerId, Long transactionId) {
        validateCustomerExists(customerId);

        CustomerTransaction entity = findOwnedTransaction(customerId, transactionId);

        if ("MONTHLY_CLOSING".equals(entity.getSourceType())) {
            throw new IllegalStateException(
                    "月次締めで作成された取引情報は削除できません。再締め処理で更新してください。"
            );
        }
        if (entity.getPaymentStatus() != null
                && entity.getPaymentStatus() != CustomerPaymentStatus.UNPAID) {
            throw new IllegalStateException(
                    "入金処理済みの取引情報は削除できません。"
            );
        }
        repository.delete(entity);
    }

    private void refreshPaymentStatus(CustomerTransaction entity) {
        int billingAmount = toZero(entity.getBillingAmount());
        int fee = toZero(entity.getFee());
        int paidAmount = toZero(entity.getPaidAmount());
        int offsetAmount = toZero(entity.getOffsetAmount());
        int adjustmentAmount = toZero(entity.getAdjustmentAmount());

        /*
         * 振込手数料と相殺は、銀行への実入金額と合わせて
         * 請求額に対する決済済み金額として扱う。
         */
        int collectedAmount;
        try {
            collectedAmount = Math.addExact(Math.addExact(
                    Math.addExact(paidAmount, fee),
                    offsetAmount
            ), adjustmentAmount);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(
                    "入金額・手数料・相殺額・その他調整額の合計が上限を超えています。",
                    exception
            );
        }
        int remainingAmount = billingAmount - collectedAmount;

        entity.setTotalAmount(collectedAmount);

        if (collectedAmount <= 0) {
            entity.setPaymentStatus(CustomerPaymentStatus.UNPAID);
        } else if (remainingAmount > 0) {
            entity.setPaymentStatus(CustomerPaymentStatus.PARTIAL);
        } else if (remainingAmount == 0) {
            entity.setPaymentStatus(CustomerPaymentStatus.PAID);
        } else {
            entity.setPaymentStatus(CustomerPaymentStatus.OVERPAID);
        }
    }

    private int toZero(Integer value) {
        return value == null ? 0 : value;
    }

    private CustomerTransaction findOwnedTransaction(
            Long customerId,
            Long transactionId) {
        @SuppressWarnings("null")
        CustomerTransaction entity = repository.findByIdAndDeletedAtIsNull(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("取引情報が見つかりません。id=" + transactionId));

        if (!customerId.equals(entity.getCustomerId())) {
            throw new IllegalArgumentException("取引情報の顧客IDが一致しません。");
        }

        return entity;
    }

    private void validate(CustomerTransactionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("CustomerTransactionRequest は必須です。");
        }

        if (request.targetMonth() == null || request.targetMonth().isBlank()) {
            throw new IllegalArgumentException("対象月は必須です。");
        }

        validateTargetMonthFormat(request.targetMonth());
        validateNonNegative("請求額", request.billingAmount());
        validateNonNegative("入金額", request.paidAmount());
        validateNonNegative("手数料", request.fee());
        validateNonNegative("相殺額", request.offsetAmount());
        validateAdjustmentAmount(request.adjustmentAmount());
        validateAdjustmentReason(request.adjustmentAmount(), request.note());
        validateNote(request.note());
    }

    private void validateClosingRequest(CustomerTransactionClosingRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("CustomerTransactionClosingRequest は必須です。");
        }

        if (request.customerId() == null) {
            throw new IllegalArgumentException("customerId は必須です。");
        }

        if (request.targetMonth() == null || request.targetMonth().isBlank()) {
            throw new IllegalArgumentException("targetMonth は必須です。");
        }

        validateTargetMonthFormat(request.targetMonth());
        validateNonNegative("請求額", request.billingAmount());
    }

    @SuppressWarnings("null")
    private void validateCustomerExists(Long customerId) {
        if (customerRepository.findByIdAndDeletedAtIsNull(customerId).isEmpty()) {
            throw new IllegalArgumentException("顧客が見つかりません。id=" + customerId);
        }
    }

    private void validateTargetMonthDuplicate(
            Long customerId,
            Long transactionId,
            String targetMonth
    ) {
        boolean exists = transactionId == null
                ? repository.existsByCustomerIdAndTargetMonthAndDeletedAtIsNull(
                        customerId,
                        targetMonth
                )
                : repository.existsByCustomerIdAndTargetMonthAndIdNotAndDeletedAtIsNull(
                        customerId,
                        targetMonth,
                        transactionId
                );
        if (exists) {
            throw new IllegalArgumentException(
                    "同じ顧客・対象月の取引情報が既に登録されています。"
            );
        }
    }

    private void validateTargetMonthFormat(String targetMonth) {
        try {
            java.time.YearMonth.parse(targetMonth);
        } catch (java.time.format.DateTimeParseException exception) {
            throw new IllegalArgumentException(
                    "対象月はyyyy-MM形式で入力してください。",
                    exception
            );
        }
    }

    private void validateNonNegative(String label, Integer value) {
        if (value != null && value < 0) {
            throw new IllegalArgumentException(label + "は0以上で入力してください。");
        }
    }

    private void validateAdjustmentAmount(Integer value) {
        if (value != null && value == Integer.MIN_VALUE) {
            throw new IllegalArgumentException("その他調整額が入力可能範囲を超えています。");
        }
    }

    private void validateAdjustmentReason(Integer adjustmentAmount, String note) {
        if (toZero(adjustmentAmount) != 0
                && (note == null || note.isBlank())) {
            throw new IllegalArgumentException(
                    "その他調整額を入力する場合は、備考へ調整理由を入力してください。"
            );
        }
    }

    private void validateNote(String note) {
        if (note != null && note.length() > 255) {
            throw new IllegalArgumentException("備考は255文字以内で入力してください。");
        }
    }
}
