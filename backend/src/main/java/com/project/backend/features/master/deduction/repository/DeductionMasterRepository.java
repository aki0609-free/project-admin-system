package com.project.backend.features.master.deduction.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.project.backend.features.master.deduction.entity.DeductionMaster;

public interface DeductionMasterRepository extends JpaRepository<DeductionMaster, Long> {
    Optional<DeductionMaster> findByDeductionCode(String deductionCode);
    boolean existsByDeductionCode(String deductionCode);
    List<DeductionMaster> findByShowOnDailyStatementTrueAndEnabledTrueOrderByDisplayOrderAscIdAsc();
    List<DeductionMaster> findByShowOnMonthlyStatementTrueAndEnabledTrueOrderByDisplayOrderAscIdAsc();
}