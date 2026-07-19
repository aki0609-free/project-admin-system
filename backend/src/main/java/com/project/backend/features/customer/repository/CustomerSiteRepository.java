package com.project.backend.features.customer.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.customer.entity.CustomerSite;

import java.util.List;

public interface CustomerSiteRepository extends JpaRepository<CustomerSite, Long> {
    List<CustomerSite> findByCustomerIdOrderByIdAsc(Long customerId);
    void deleteByCustomerId(Long customerId);
}