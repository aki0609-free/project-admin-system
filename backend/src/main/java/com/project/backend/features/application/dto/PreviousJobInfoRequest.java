package com.project.backend.features.application.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PreviousJobInfoRequest {
    private String previousJob;
    private String previousJobPeriod;
    private String insuredBefore;
    private String dormitoryExperience;
    private String previousRent;
}