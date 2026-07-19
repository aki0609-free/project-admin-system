package com.project.backend.features.application.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DormitoryInfoResponse {
    private String needsDormitory;
    private String roomType;
    private String rent;
}
