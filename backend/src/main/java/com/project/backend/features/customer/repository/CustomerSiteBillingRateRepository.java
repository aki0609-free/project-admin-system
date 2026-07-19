package com.project.backend.features.customer.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.backend.features.customer.entity.CustomerSiteBillingRate;

public interface CustomerSiteBillingRateRepository
        extends JpaRepository<CustomerSiteBillingRate, Long> {

    List<CustomerSiteBillingRate>
            findByCustomerSiteCustomerIdAndDeletedAtIsNullOrderByCustomerSiteIdAscDisplayOrderAscIdAsc(
                    Long customerId
            );

    List<CustomerSiteBillingRate>
            findByCustomerSiteIdAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc(
                    Long customerSiteId
            );

    Optional<CustomerSiteBillingRate>
            findByIdAndDeletedAtIsNull(Long id);

    @Query("""
            select r
            from CustomerSiteBillingRate r
            where r.customerSite.id = :customerSiteId
              and r.jobCode = :jobCode
              and r.siteRoleCode = :siteRoleCode
              and r.activeFlag = true
              and r.deletedAt is null
              and r.effectiveFrom <= :workDate
              and (
                    r.effectiveTo is null
                    or r.effectiveTo >= :workDate
                  )
            order by r.effectiveFrom desc, r.id desc
            """)
    List<CustomerSiteBillingRate> findApplicableRates(
            @Param("customerSiteId") Long customerSiteId,
            @Param("jobCode") String jobCode,
            @Param("siteRoleCode") String siteRoleCode,
            @Param("workDate") LocalDate workDate
    );

    boolean existsByCustomerSiteIdAndJobCodeAndSiteRoleCodeAndEffectiveFromAndDeletedAtIsNull(
            Long customerSiteId,
            String jobCode,
            String siteRoleCode,
            LocalDate effectiveFrom
    );

    boolean existsByCustomerSiteIdAndJobCodeAndSiteRoleCodeAndEffectiveFromAndIdNotAndDeletedAtIsNull(
            Long customerSiteId,
            String jobCode,
            String siteRoleCode,
            LocalDate effectiveFrom,
            Long id
    );
}