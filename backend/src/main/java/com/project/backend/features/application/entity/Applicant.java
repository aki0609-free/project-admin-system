package com.project.backend.features.application.entity;

import java.time.LocalDate;

import com.project.backend.app.base.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "applicants",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"tenant_id", "application_no"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Applicant extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 応募者番号 */
    @Column(name = "application_no", nullable = false)
    private String applicationNo;

    /** 雇用形態 */
    @Column(name = "contract_type")
    private String contractType;

    /** 氏名 */
    @Column(nullable = false)
    private String name;

    /** フリガナ */
    @Column(name = "furigana_name")
    private String furiganaName;

    /** 生年月日 */
    @Column(name = "birth_date")
    private LocalDate birthDate;

    /** 性別 */
    private String gender;

    /** 問い合わせ日 */
    @Column(name = "contact_date")
    private LocalDate contactDate;

    /** 採用状況 */
    @Column(name = "recruitment_status")
    private String recruitmentStatus;

    /** 日給（入社時） */
    @Column(name = "daily_wage")
    private String dailyWage;

    /** 入社日（社保無） */
    @Column(name = "employment_date")
    private LocalDate employmentDate;

    /** 入社日（社保有） */
    @Column(name = "employment_date_with_insurance")
    private LocalDate employmentDateWithInsurance;

    /** 退社日（社保無） */
    @Column(name = "termination_date")
    private LocalDate terminationDate;

    /** 退社日（社保有） */
    @Column(name = "termination_date_with_insurance")
    private LocalDate terminationDateWithInsurance;

    /** 職種 */
    @Column(name = "job_category")
    private String jobCategory;

    /** 退職前得意先 */
    @Column(name = "previous_client")
    private String previousClient;

    /** 退職状況 */
    @Column(name = "resignation_status")
    private String resignationStatus;

    /** 退職理由 */
    @Column(name = "resignation_reason")
    private String resignationReason;

    /** 応募理由1 */
    @Column(name = "application_reason_1")
    private String applicationReason1;

    /** 応募理由2 */
    @Column(name = "application_reason_2")
    private String applicationReason2;

    @Column(name = "recruitment_company")
    private String recruitmentCompany;

    @Column(name = "media_type")
    private String mediaType;

    @Embedded
    private DormitoryInfo dormitoryInfo;

    @Embedded
    private SearchKeywordInfo searchKeywordInfo;

    @Embedded
    private PreviousJobInfo previousJobInfo;

    /**
     * 応募媒体参照
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_media_id")
    private ApplicationMedia applicationMedia;

    /**
     * 応募時点スナップショット
     */
    @Column(name = "media_name_snapshot")
    private String mediaNameSnapshot;

    @Column(name = "media_year_month_snapshot")
    private String mediaYearMonthSnapshot;
}