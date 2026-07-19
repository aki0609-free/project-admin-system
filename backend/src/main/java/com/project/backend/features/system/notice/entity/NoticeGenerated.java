package com.project.backend.features.system.notice.entity;

import java.time.LocalDate;

import com.project.backend.app.base.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "notice_generated",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {
                                "tenant_id",
                                "rule_code",
                                "target_table_name",
                                "target_key",
                                "target_date"
                        }
                )
        }
)
@Getter
@Setter
public class NoticeGenerated extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_code", nullable = false, length = 100)
    private String ruleCode;

    @Column(name = "target_table_name", nullable = false, length = 200)
    private String targetTableName;

    @Column(name = "target_key", nullable = false, length = 255)
    private String targetKey;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @Column(name = "generated_notice_id")
    private Long generatedNoticeId;
}