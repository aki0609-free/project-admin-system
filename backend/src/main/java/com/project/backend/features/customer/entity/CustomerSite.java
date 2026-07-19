package com.project.backend.features.customer.entity;

import java.util.ArrayList;
import java.util.List;

import com.project.backend.app.base.entity.BaseEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "customer_sites")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerSite extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "contact_person_name", length = 255)
    private String contactPersonName;

    @Column(name = "contact_person_phone", length = 50)
    private String contactPersonPhone;

    @Column(name = "contact_person_email", length = 255)
    private String contactPersonEmail;

    @Column(name = "distance_from_company_km")
    private Integer distanceFromCompanyKm;

    @Builder.Default
    @OneToMany(
            mappedBy = "customerSite",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @OrderBy("displayOrder ASC, id ASC")
    private List<CustomerSiteBillingRate> billingRates = new ArrayList<>();

    public void addBillingRate(CustomerSiteBillingRate billingRate) {
        billingRate.setCustomerSite(this);
        billingRates.add(billingRate);
    }

    public void removeBillingRate(CustomerSiteBillingRate billingRate) {
        billingRates.remove(billingRate);
        billingRate.setCustomerSite(null);
    }

    public void clearBillingRates() {
        for (CustomerSiteBillingRate billingRate : billingRates) {
            billingRate.setCustomerSite(null);
        }
        billingRates.clear();
    }
}