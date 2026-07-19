package com.project.backend.features.application.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SearchKeywordInfoResponse {
    private String searchPlace;
    private String searchWebsite;
    private String searchKeyword;
    private String desiredNextJob;
}
