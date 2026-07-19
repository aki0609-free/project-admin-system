package com.project.backend.features.system.company.entity;

import java.math.BigDecimal;

import com.project.backend.app.base.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "company_profile", uniqueConstraints = {
        @UniqueConstraint(columnNames = {
                "tenant_id",
                "company_code"
        })
})
@Getter
@Setter
public class CompanyProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_code", nullable = false, length = 100)
    private String companyCode;

    @Column(name = "company_name", nullable = false, length = 255)
    private String companyName;

    @Column(name = "company_name_kana", length = 255)
    private String companyNameKana;

    @Column(name = "short_name", length = 100)
    private String shortName;

    @Column(name = "representative_title", length = 100)
    private String representativeTitle;

    @Column(name = "representative_name", length = 255)
    private String representativeName;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "prefecture", length = 100)
    private String prefecture;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "address_line1", length = 255)
    private String addressLine1;

    @Column(name = "address_line2", length = 255)
    private String addressLine2;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "fax", length = 50)
    private String fax;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "website_url", length = 500)
    private String websiteUrl;

    @Column(name = "capital_amount", precision = 15, scale = 2)
    private BigDecimal capitalAmount;

    /**
     * 代表的な許可番号。
     *
     * 例:
     * 神奈川県知事 許可（般－1）第87038号
     */
    @Column(name = "permit_number", length = 255)
    private String permitNumber;

    /**
     * 適格請求書発行事業者登録番号。
     */
    @Column(name = "qualified_invoice_issuer_number", length = 50)
    private String qualifiedInvoiceIssuerNumber;

    @Column(name = "service_area", length = 500)
    private String serviceArea;

    @Lob
    @Column(name = "business_description", columnDefinition = "LONGTEXT")
    private String businessDescription;

    @Lob
    @Column(name = "certification_information", columnDefinition = "LONGTEXT")
    private String certificationInformation;

    @Lob
    @Column(name = "invoice_note", columnDefinition = "LONGTEXT")
    private String invoiceNote;

    @Column(name = "invoice_bank_name", length = 255)
    private String invoiceBankName;

    @Column(name = "invoice_bank_branch_name", length = 255)
    private String invoiceBankBranchName;

    @Column(name = "invoice_bank_account_type", length = 50)
    private String invoiceBankAccountType;

    @Column(name = "invoice_bank_account_number", length = 100)
    private String invoiceBankAccountNumber;

    @Column(name = "invoice_bank_account_holder", length = 255)
    private String invoiceBankAccountHolder;

    @Column(name = "active_flag", nullable = false)
    private Boolean activeFlag = true;
}