package com.project.backend.features.application.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApplicantCreateRequest {
    @NotNull
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

    private DormitoryInfoRequest dormitoryInfo;
    private SearchKeywordInfoRequest searchKeywordInfo;
    private PreviousJobInfoRequest previousJobInfo;
    private ApplicationMediaLinkRequest applicationMedia;
}