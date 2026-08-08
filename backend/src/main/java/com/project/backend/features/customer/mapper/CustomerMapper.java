package com.project.backend.features.customer.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.project.backend.common.dayrule.dto.DayRule;
import com.project.backend.common.dayrule.utils.DayRuleUtils;
import com.project.backend.features.customer.dto.*;
import com.project.backend.features.customer.entity.Customer;
import com.project.backend.features.customer.entity.CustomerEmployee;
import com.project.backend.features.customer.entity.CustomerSite;
import com.project.backend.features.customer.enums.CustomerInvoiceType;

@Component
public class CustomerMapper {

    public Customer toEntity(
            CustomerSaveRequest request
    ) {
        Customer entity = new Customer();
        apply(entity, request);
        return entity;
    }

    public void apply(
            Customer entity,
            CustomerSaveRequest request
    ) {
        entity.setName(request.name());
        entity.setFuriganaName(request.furiganaName());
        entity.setShortName(request.shortName());
        entity.setPostNo(request.postNo());
        entity.setAddress(request.address());
        entity.setRepresentativeName(request.representativeName());
        entity.setPhone(request.phone());
        entity.setJobType(request.jobType());
        entity.setContractFlag(request.contractFlag());

        entity.setInvoiceType(
                request.invoiceType() != null
                        ? request.invoiceType()
                        : CustomerInvoiceType.PATTERN_1
        );

        applyClosingDayRule(
                entity,
                request.closingDayRule()
        );

        applyPaymentDayRule(
                entity,
                request.paymentDayRule()
        );
    }

    public CustomerListItemResponse toListItem(
            Customer customer,
            int siteCount,
            int employeeCount,
            String latestPaymentStatus
    ) {
        return new CustomerListItemResponse(
                customer.getId(),
                customer.getName(),
                customer.getFuriganaName(),
                customer.getShortName(),
                customer.getPostNo(),
                customer.getAddress(),
                customer.getRepresentativeName(),
                customer.getPhone(),
                customer.getJobType(),
                customer.getContractFlag(),

                normalizeInvoiceType(
                        customer.getInvoiceType()
                ),

                DayRuleUtils.toResponse(
                        customer.getClosingDayType(),
                        customer.getClosingDayValue(),
                        customer.getClosingMonthOffset()
                ),

                DayRuleUtils.toResponse(
                        customer.getPaymentDayType(),
                        customer.getPaymentDayValue(),
                        customer.getPaymentMonthOffset()
                ),

                siteCount,
                employeeCount,
                latestPaymentStatus
        );
    }

    public CustomerDetailResponse toDetail(
            Customer customer,
            List<CustomerSite> sites,
            List<CustomerEmployee> employees,
            String latestPaymentStatus
    ) {
        List<CustomerSite> safeSites =
                sites == null
                        ? List.of()
                        : sites;

        List<CustomerEmployee> safeEmployees =
                employees == null
                        ? List.of()
                        : employees;

        return new CustomerDetailResponse(
                customer.getId(),
                customer.getName(),
                customer.getFuriganaName(),
                customer.getShortName(),
                customer.getPostNo(),
                customer.getAddress(),
                customer.getRepresentativeName(),
                customer.getPhone(),
                customer.getJobType(),
                customer.getContractFlag(),

                normalizeInvoiceType(
                        customer.getInvoiceType()
                ),

                DayRuleUtils.toResponse(
                        customer.getClosingDayType(),
                        customer.getClosingDayValue(),
                        customer.getClosingMonthOffset()
                ),

                DayRuleUtils.toResponse(
                        customer.getPaymentDayType(),
                        customer.getPaymentDayValue(),
                        customer.getPaymentMonthOffset()
                ),

                safeSites.size(),
                safeEmployees.size(),
                latestPaymentStatus,

                safeSites.stream()
                        .map(this::toSiteResponse)
                        .toList(),

                safeEmployees.stream()
                        .map(this::toEmployeeResponse)
                        .toList()
        );
    }

    private CustomerInvoiceType normalizeInvoiceType(
            CustomerInvoiceType invoiceType
    ) {
        return invoiceType != null
                ? invoiceType
                : CustomerInvoiceType.PATTERN_1;
    }

    private void applyClosingDayRule(
            Customer entity,
            DayRule rule
    ) {
        entity.setClosingDayType(
                rule == null
                        ? null
                        : rule.type()
        );

        entity.setClosingDayValue(
                rule == null
                        ? null
                        : rule.value()
        );

        entity.setClosingMonthOffset(
                rule == null || rule.monthOffset() == null
                        ? 0
                        : rule.monthOffset()
        );
    }

    private void applyPaymentDayRule(
            Customer entity,
            DayRule rule
    ) {
        entity.setPaymentDayType(
                rule == null
                        ? null
                        : rule.type()
        );

        entity.setPaymentDayValue(
                rule == null
                        ? null
                        : rule.value()
        );

        entity.setPaymentMonthOffset(
                rule == null || rule.monthOffset() == null
                        ? 0
                        : rule.monthOffset()
        );
    }

    public CustomerSite toSiteEntity(
            Long customerId,
            CustomerSiteRequest request
    ) {
        CustomerSite entity = new CustomerSite();
        entity.setCustomerId(customerId);
        applySite(entity, request);
        return entity;
    }

    public void applySite(
            CustomerSite entity,
            CustomerSiteRequest request
    ) {
        entity.setName(request.name());
        entity.setContactPersonName(
                request.contactPersonName()
        );
        entity.setContactPersonPhone(
                request.contactPersonPhone()
        );
        entity.setContactPersonEmail(
                request.contactPersonEmail()
        );
        entity.setDistanceFromCompanyKm(
                request.distanceFromCompanyKm()
        );
    }

    public CustomerSiteResponse toSiteResponse(
            CustomerSite entity
    ) {
        return new CustomerSiteResponse(
                entity.getId(),
                entity.getCustomerId(),
                entity.getName(),
                entity.getContactPersonName(),
                entity.getContactPersonPhone(),
                entity.getContactPersonEmail(),
                entity.getDistanceFromCompanyKm()
        );
    }

    public CustomerEmployee toEmployeeEntity(
            Long customerId,
            CustomerEmployeeRequest request
    ) {
        CustomerEmployee entity =
                new CustomerEmployee();

        entity.setCustomerId(customerId);

        applyEmployee(
                entity,
                request
        );

        return entity;
    }

    public void applyEmployee(
            CustomerEmployee entity,
            CustomerEmployeeRequest request
    ) {
        entity.setName(request.name());
        entity.setFuriganaName(
                request.furiganaName()
        );
        entity.setPosition(
                request.position()
        );
        entity.setPhone(
                request.phone()
        );
        entity.setEmail(
                request.email()
        );
        entity.setInvoiceToFlag(
                Boolean.TRUE.equals(
                        request.invoiceToFlag()
                )
        );
        entity.setInvoiceCcFlag(
                Boolean.TRUE.equals(
                        request.invoiceCcFlag()
                )
        );
    }

    public CustomerEmployeeResponse toEmployeeResponse(
            CustomerEmployee entity
    ) {
        return new CustomerEmployeeResponse(
                entity.getId(),
                entity.getCustomerId(),
                entity.getName(),
                entity.getFuriganaName(),
                entity.getPosition(),
                entity.getPhone(),
                entity.getEmail(),
                Boolean.TRUE.equals(
                        entity.getInvoiceToFlag()
                ),
                Boolean.TRUE.equals(
                        entity.getInvoiceCcFlag()
                )
        );
    }
}
