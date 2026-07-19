package com.project.backend.features.application.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PreviousJobInfoResponse {
    private String previousJob;
    private String previousJobPeriod;
    private String insuredBefore;
    private String dormitoryExperience;
    private String previousRent;
}
