package com.project.backend.features.tax.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.tax.entity.StandardSalary;

public interface StandardSalaryRepository
        extends JpaRepository<StandardSalary, Long> {

    List<StandardSalary> findAllByOrderByMinSalaryAsc();
}