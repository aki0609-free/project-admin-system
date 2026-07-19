package com.project.backend.features.dashboard.entity;

import java.time.LocalDate;

import com.project.backend.app.base.entity.BaseEntity;
import com.project.backend.features.dashboard.enums.NoticeContentFormat;
import com.project.backend.features.dashboard.enums.NoticeSourceType;
import com.project.backend.features.dashboard.enums.NoticeType;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "notices")
@Getter
@Setter
public class Notice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NoticeType type = NoticeType.INFO;

    @Column(length = 50)
    private String color = "blue";

    @Enumerated(EnumType.STRING)
    @Column(name = "content_format", nullable = false, length = 30)
    private NoticeContentFormat contentFormat = NoticeContentFormat.PLAIN_TEXT;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "link_url", length = 1000)
    private String linkUrl;

    @Column(name = "link_label", length = 200)
    private String linkLabel;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private NoticeSourceType sourceType = NoticeSourceType.MANUAL;

    @Column(name = "source_rule_code", length = 100)
    private String sourceRuleCode;

    @Column(name = "pinned_flag", nullable = false)
    private boolean pinnedFlag = false;

    @Column(name = "active_flag", nullable = false)
    private boolean activeFlag = true;
}