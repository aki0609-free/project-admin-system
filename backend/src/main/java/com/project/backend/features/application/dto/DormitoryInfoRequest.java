package com.project.backend.features.application.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DormitoryInfoRequest {
    private String needsDormitory;
    private String roomType;
    private String rent;
}
