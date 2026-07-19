package com.project.backend.features.customer.entity;

import com.project.backend.app.base.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "customer_employees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerEmployee extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(nullable = false)
    private String name;

    @Column(name = "furigana_name")
    private String furiganaName;

    private String position;

    private String phone;

    private String email;

    @Column(name = "invoice_to_flag")
    private Boolean invoiceToFlag;

    @Column(name = "invoice_cc_flag")
    private Boolean invoiceCcFlag;
}