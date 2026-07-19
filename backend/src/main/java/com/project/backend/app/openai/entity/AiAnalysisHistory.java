package com.project.backend.app.openai.entity;

import com.project.backend.app.base.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ai_analysis_history")
@Getter
@Setter
public class AiAnalysisHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String analysisType;

    @Column(nullable = false)
    private String subjectId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String promptVersion;

    @Column(nullable = false)
    private String model;

    @Lob
    @Column(nullable = false)
    private String summaryJson;

    @Lob
    @Column(nullable = false)
    private String responseJson;

}