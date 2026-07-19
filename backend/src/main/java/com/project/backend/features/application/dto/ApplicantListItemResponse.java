package com.project.backend.features.application.dto;

import java.time.LocalDate;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApplicantListItemResponse {
    private Long id;
    private String applicationNo;

    private String contractType;
    private String name;
    private String furiganaName;
    private LocalDate birthDate;
    private String gender;

    private LocalDate contactDate;
    private String recruitmentStatus;
    private String dailyWage;

    private LocalDate employmentDate;
    private LocalDate employmentDateWithInsurance;
    private LocalDate terminationDate;
    private LocalDate terminationDateWithInsurance;

    private String jobCategory;
    private String previousClient;
    private String resignationStatus;
    private String resignationReason;

    private String applicationReason1;
    private String applicationReason2;

    private String recruitmentCompany;
    private String mediaType;

    private DormitoryInfoResponse dormitoryInfo;
    private SearchKeywordInfoResponse searchKeywordInfo;
    private PreviousJobInfoResponse previousJobInfo;

    private ApplicationMediaLinkResponse applicationMedia;
}