package com.project.backend.features.system.notice.entity;

import com.project.backend.app.base.entity.BaseEntity;
import com.project.backend.features.dashboard.enums.NoticeContentFormat;
import com.project.backend.features.system.notice.enums.NoticeDateType;
import com.project.backend.features.system.notice.enums.NoticeSeverity;
import com.project.backend.features.system.notice.enums.NoticeTargetDateSourceType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "notice_rule",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"tenant_id", "rule_code"})
        }
)
@Getter
@Setter
public class NoticeRule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_code", nullable = false, length = 100)
    private String ruleCode;

    @Column(name = "rule_name", nullable = false, length = 200)
    private String ruleName;

    @Column(name = "target_table_name", nullable = false, length = 200)
    private String targetTableName;

    @Column(name = "target_key_column_name", nullable = false, length = 200)
    private String targetKeyColumnName;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_date_source_type", nullable = false, length = 30)
    private NoticeTargetDateSourceType targetDateSourceType =
            NoticeTargetDateSourceType.DATE_COLUMN;

    @Column(name = "target_date_column_name", length = 200)
    private String targetDateColumnName;

    @Column(name = "target_day_type_column_name", length = 200)
    private String targetDayTypeColumnName;

    @Column(name = "target_day_value_column_name", length = 200)
    private String targetDayValueColumnName;

    @Column(name = "target_label_column_name", length = 200)
    private String targetLabelColumnName;

    @Column(name = "where_clause", length = 1000)
    private String whereClause;

    @Column(name = "notice_title_template", nullable = false, length = 500)
    private String noticeTitleTemplate;

    @Lob
    @Column(name = "notice_body_template", nullable = false)
    private String noticeBodyTemplate;

    @Enumerated(EnumType.STRING)
    @Column(name = "notice_content_format", nullable = false, length = 30)
    private NoticeContentFormat noticeContentFormat =
            NoticeContentFormat.PLAIN_TEXT;

    @Enumerated(EnumType.STRING)
    @Column(name = "notice_severity", nullable = false, length = 30)
    private NoticeSeverity noticeSeverity = NoticeSeverity.INFO;

    @Enumerated(EnumType.STRING)
    @Column(name = "date_type", nullable = false, length = 30)
    private NoticeDateType dateType = NoticeDateType.BEFORE_DAYS;

    @Column(name = "days_before")
    private Integer daysBefore;

    @Column(name = "day_of_month")
    private Integer dayOfMonth;

    @Column(name = "cron_expression", length = 100)
    private String cronExpression;

    @Column(name = "active_flag", nullable = false)
    private boolean activeFlag = true;
}