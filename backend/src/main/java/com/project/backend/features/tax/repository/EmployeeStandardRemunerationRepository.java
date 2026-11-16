package com.project.backend.features.tax.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.tax.entity.EmployeeStandardRemuneration;

public interface EmployeeStandardRemunerationRepository
        extends JpaRepository<EmployeeStandardRemuneration, Long> {

    List<EmployeeStandardRemuneration>
            findAllByDeletedAtIsNullOrderByEmployeeIdAscEffectiveFromDesc();
}
