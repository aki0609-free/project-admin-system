package com.project.backend.features.master.allowance.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.master.allowance.entity.AllowanceMaster;

public interface AllowanceMasterRepository extends JpaRepository<AllowanceMaster, Long> {
    Optional<AllowanceMaster> findByAllowanceCode(String allowanceCode);
    boolean existsByAllowanceCode(String allowanceCode);
    List<AllowanceMaster> findByShowOnDailyStatementTrueAndEnabledTrueOrderByDisplayOrderAscIdAsc();
    List<AllowanceMaster> findByShowOnMonthlyStatementTrueAndEnabledTrueOrderByDisplayOrderAscIdAsc();
}