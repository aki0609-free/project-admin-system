package com.project.backend.features.customer.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.customer.entity.CustomerSite;

import java.util.List;
import java.util.Optional;

public interface CustomerSiteRepository extends JpaRepository<CustomerSite, Long> {
    Optional<CustomerSite> findByIdAndDeletedAtIsNull(Long id);
    List<CustomerSite> findByDeletedAtIsNullOrderByIdAsc();
    List<CustomerSite> findByCustomerIdAndDeletedAtIsNullOrderByIdAsc(Long customerId);
}
