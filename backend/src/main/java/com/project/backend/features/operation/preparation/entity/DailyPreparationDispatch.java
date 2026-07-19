package com.project.backend.features.operation.preparation.entity;

import com.project.backend.app.base.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "daily_preparation_dispatches",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"tenant_id", "preparation_id", "customer_site_id"})
        }
)
@Getter
@Setter
public class DailyPreparationDispatch extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "preparation_id", nullable = false)
    private Long preparationId;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "customer_site_id")
    private Long customerSiteId;

    @Column(name = "customer_name", length = 255)
    private String customerName;

    @Column(name = "site_name", length = 255)
    private String siteName;

    @Column(name = "distance_from_company_km")
    private Integer distanceFromCompanyKm;

    @Column(name = "vehicle_count", nullable = false)
    private Integer vehicleCount = 0;

    @Column(name = "note", length = 1000)
    private String note;
}