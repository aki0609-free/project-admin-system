package com.project.backend.features.tax.entity;

import java.math.BigDecimal;

import com.project.backend.features.tax.enums.InsuranceType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "insurance_rate_master")
@Getter
@Setter
public class InsuranceRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "insurance_type", nullable = false, length = 50)
    private InsuranceType insuranceType;

    @Column(nullable = false)
    private Integer year;

    @Column(precision = 6, scale = 5, nullable = false)
    private BigDecimal employeeRate;

    @Column(precision = 6, scale = 5)
    private BigDecimal employerRate;

}
