package com.project.backend.features.customer.mapper;

import org.springframework.stereotype.Component;

import com.project.backend.common.dayrule.dto.DayRule;
import com.project.backend.common.dayrule.utils.DayRuleUtils;
import com.project.backend.features.customer.dto.CustomerTransactionClosingRequest;
import com.project.backend.features.customer.dto.CustomerTransactionRequest;
import com.project.backend.features.customer.dto.CustomerTransactionResponse;
import com.project.backend.features.customer.entity.CustomerTransaction;
import com.project.backend.features.customer.enums.CustomerPaymentStatus;

@Component
public class CustomerTransactionMapper {

    public CustomerTransaction toEntity(
            Long customerId,
            CustomerTransactionRequest request
    ) {
        CustomerTransaction entity = new CustomerTransaction();
        entity.setCustomerId(customerId);
        apply(entity, request);
        return entity;
    }

    public void apply(
            CustomerTransaction entity,
            CustomerTransactionRequest request
    ) {
        entity.setTargetMonth(request.targetMonth());

        applyClosingDayRule(entity, request.closingDayRule());
        applyPaymentDayRule(entity, request.paymentDayRule());

        entity.setBillingAmount(request.billingAmount());
        entity.setExpectedPaymentDate(request.expectedPaymentDate());
        entity.setConfirmedPaymentDate(request.confirmedPaymentDate());

        entity.setPaidAmount(request.paidAmount());
        entity.setFee(request.fee());
        entity.setOffsetAmount(request.offsetAmount());
        entity.setTotalAmount(request.totalAmount());

        entity.setPaymentStatus(
                request.paymentStatus() == null
                        ? CustomerPaymentStatus.UNPAID
                        : request.paymentStatus()
        );

        entity.setNote(request.note());
    }

    public void applyFromClosing(
            CustomerTransaction entity,
            CustomerTransactionClosingRequest request
    ) {
        entity.setCustomerId(request.customerId());
        entity.setTargetMonth(request.targetMonth());

        applyClosingDayRule(entity, request.closingDayRule());
        applyPaymentDayRule(entity, request.paymentDayRule());

        entity.setBillingAmount(request.billingAmount());
        entity.setExpectedPaymentDate(request.expectedPaymentDate());

        if (entity.getPaidAmount() == null) {
            entity.setPaidAmount(0);
        }

        if (entity.getFee() == null) {
            entity.setFee(0);
        }

        if (entity.getOffsetAmount() == null) {
            entity.setOffsetAmount(0);
        }

        if (entity.getTotalAmount() == null) {
            entity.setTotalAmount(0);
        }

        if (entity.getPaymentStatus() == null) {
            entity.setPaymentStatus(CustomerPaymentStatus.UNPAID);
        }

        entity.setNote(request.note());
    }

    public CustomerTransactionResponse toResponse(
            CustomerTransaction entity
    ) {
        return new CustomerTransactionResponse(
                entity.getId(),
                entity.getCustomerId(),
                entity.getTargetMonth(),

                DayRuleUtils.toResponse(
                        entity.getClosingDayType(),
                        entity.getClosingDayValue(),
                        entity.getClosingMonthOffset()
                ),

                DayRuleUtils.toResponse(
                        entity.getPaymentDayType(),
                        entity.getPaymentDayValue(),
                        entity.getPaymentMonthOffset()
                ),

                entity.getBillingAmount(),
                entity.getExpectedPaymentDate(),
                entity.getConfirmedPaymentDate(),
                entity.getPaidAmount(),
                entity.getFee(),
                entity.getOffsetAmount(),
                entity.getTotalAmount(),

                entity.getPaymentStatus() == null
                        ? CustomerPaymentStatus.UNPAID.name()
                        : entity.getPaymentStatus().name(),

                entity.getNote()
        );
    }

    private void applyClosingDayRule(CustomerTransaction entity, DayRule rule) {
        entity.setClosingDayType(rule == null ? null : rule.type());
        entity.setClosingDayValue(rule == null ? null : rule.value());
        entity.setClosingMonthOffset(rule == null ? 0 : rule.monthOffset());
    }

    private void applyPaymentDayRule(CustomerTransaction entity, DayRule rule) {
        entity.setPaymentDayType(rule == null ? null : rule.type());
        entity.setPaymentDayValue(rule == null ? null : rule.value());
        entity.setPaymentMonthOffset(rule == null ? 0 : rule.monthOffset());
    }
}