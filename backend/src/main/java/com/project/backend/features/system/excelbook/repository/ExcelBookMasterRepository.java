package com.project.backend.features.system.excelbook.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.system.excelbook.entity.ExcelBookMaster;

public interface ExcelBookMasterRepository extends JpaRepository<ExcelBookMaster, Long> {

    List<ExcelBookMaster> findByDeletedAtIsNullOrderByIdDesc();

    Optional<ExcelBookMaster> findByIdAndDeletedAtIsNull(Long id);

    Optional<ExcelBookMaster> findFirstByBookCodeAndActiveFlagTrueAndDeletedAtIsNull(String bookCode);

    boolean existsByBookCodeAndDeletedAtIsNull(String bookCode);

    boolean existsByBookCodeAndIdNotAndDeletedAtIsNull(String bookCode, Long id);
}