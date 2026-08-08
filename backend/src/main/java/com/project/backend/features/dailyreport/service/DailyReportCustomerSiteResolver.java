package com.project.backend.features.dailyreport.service;

import org.springframework.stereotype.Component;

import com.project.backend.features.customer.entity.Customer;
import com.project.backend.features.customer.entity.CustomerSite;
import com.project.backend.features.customer.repository.CustomerRepository;
import com.project.backend.features.customer.repository.CustomerSiteRepository;
import com.project.backend.features.dailyreport.dto.DailyReportSaveRequest;
import com.project.backend.features.dailyreport.entity.DailyReport;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DailyReportCustomerSiteResolver {

    private final CustomerRepository customerRepository;
    private final CustomerSiteRepository customerSiteRepository;

    public void applySnapshot(
            DailyReport report,
            DailyReportSaveRequest request
    ) {
        if (request.customerSiteId() != null) {
            CustomerSite site = customerSiteRepository
                    .findByIdAndDeletedAtIsNull(request.customerSiteId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "現場が見つかりません。customerSiteId="
                                    + request.customerSiteId()
                    ));

            if (request.customerId() != null
                    && !request.customerId().equals(site.getCustomerId())) {
                throw new IllegalArgumentException(
                        "指定された顧客と現場の組み合わせが一致しません。"
                );
            }

            Customer customer = findCustomer(site.getCustomerId());
            report.setCustomerId(customer.getId());
            report.setCustomerName(customer.getName());
            report.setCustomerSiteId(site.getId());
            report.setSiteName(site.getName());
            return;
        }

        report.setCustomerSiteId(null);
        report.setSiteName(null);

        if (request.customerId() != null) {
            Customer customer = findCustomer(request.customerId());
            report.setCustomerId(customer.getId());
            report.setCustomerName(customer.getName());
            return;
        }

        report.setCustomerId(null);
        report.setCustomerName(null);
    }

    private Customer findCustomer(Long customerId) {
        return customerRepository.findByIdAndDeletedAtIsNull(customerId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "顧客が見つかりません。customerId=" + customerId
                ));
    }
}
