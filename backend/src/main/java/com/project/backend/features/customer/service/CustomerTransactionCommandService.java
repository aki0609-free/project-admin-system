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

        return repository.save(
                mapper.toEntity(customerId, request)).getId();
    }

    public Long upsertFromMonthlyClosing(CustomerTransactionClosingRequest request) {
        validateClosingRequest(request);
        validateCustomerExists(request.customerId());

        CustomerTransaction entity = repository
                .findByCustomerIdAndTargetMonth(request.customerId(), request.targetMonth())
                .orElseGet(CustomerTransaction::new);

        if (entity.getPaymentStatus() == CustomerPaymentStatus.PAID) {
            throw new IllegalStateException(
                    "入金済みの取引は月次締め処理から更新できません。customerId="
                            + request.customerId()
                            + ", targetMonth="
                            + request.targetMonth());
        }

        mapper.applyFromClosing(entity, request);

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

        entity.setConfirmedPaymentDate(request.confirmedPaymentDate());
        entity.setPaidAmount(toZero(request.paidAmount()));
        entity.setFee(toZero(request.fee()));
        entity.setOffsetAmount(toZero(request.offsetAmount()));

        refreshPaymentStatus(entity);

        if (request.note() != null && !request.note().isBlank()) {
            entity.setNote(request.note());
        }

        repository.save(entity);
    }

    @SuppressWarnings("null")
    public void delete(Long customerId, Long transactionId) {
        validateCustomerExists(customerId);

        CustomerTransaction entity = findOwnedTransaction(customerId, transactionId);
        repository.delete(entity);
    }

    private void refreshPaymentStatus(CustomerTransaction entity) {
        int billingAmount = toZero(entity.getBillingAmount());
        int fee = toZero(entity.getFee());
        int paidAmount = toZero(entity.getPaidAmount());
        int offsetAmount = toZero(entity.getOffsetAmount());

        int receivableAmount = billingAmount + fee;
        int collectedAmount = paidAmount + offsetAmount;
        int remainingAmount = receivableAmount - collectedAmount;

        entity.setTotalAmount(remainingAmount);

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
        CustomerTransaction entity = repository.findById(transactionId)
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
    }

    @SuppressWarnings("null")
    private void validateCustomerExists(Long customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new IllegalArgumentException("顧客が見つかりません。id=" + customerId);
        }
    }
}