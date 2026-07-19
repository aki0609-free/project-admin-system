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
public class SearchKeywordInfo {

    /** どこで */
    @Column(name = "search_place")
    private String searchPlace;

    /** 検索サイト */
    @Column(name = "search_website")
    private String searchWebsite;

    /** キーワード */
    @Column(name = "search_keyword")
    private String searchKeyword;

    /** 転職先 */
    @Column(name = "desired_next_job")
    private String desiredNextJob;
}