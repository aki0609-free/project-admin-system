package com.project.backend.features.application.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SearchKeywordInfoRequest {
    private String searchPlace;
    private String searchWebsite;
    private String searchKeyword;
    private String desiredNextJob;
}
