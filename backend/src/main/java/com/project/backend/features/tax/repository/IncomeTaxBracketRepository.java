package com.project.backend.features.tax.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.tax.entity.IncomeTaxBracket;

public interface IncomeTaxBracketRepository extends JpaRepository<IncomeTaxBracket, Long> {
    void deleteByYear(int year);
    List<IncomeTaxBracket> findByYearOrderByMinSalaryAscDependentsAsc(Integer year);
}
