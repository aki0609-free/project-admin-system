package com.project.backend.features.application.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreviousJobInfo {

    /** 前職種 */
    @Column(name = "previous_job")
    private String previousJob;

    /** 前職在籍期間 */
    @Column(name = "previous_job_period")
    private String previousJobPeriod;

    /** 前職社保加入有無 */
    @Column(name = "insured_before")
    private String insuredBefore;

    /** 社員寮経験有無 */
    @Column(name = "dormitory_experience")
    private String dormitoryExperience;

    /** 前職寮費 */
    @Column(name = "previous_rent")
    private String previousRent;
}