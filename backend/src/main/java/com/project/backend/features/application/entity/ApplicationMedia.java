package com.project.backend.features.application.entity;

import java.math.BigDecimal;
import java.time.YearMonth;

import com.project.backend.app.base.entity.BaseEntity;
import com.project.backend.common.converter.YearMonthConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "application_media",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"tenant_id", "media_name", "media_year_month"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationMedia extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 応募媒体名 */
    @Column(name = "media_name", nullable = false, length = 100)
    private String mediaName;

    /** 掲載地域 */
    @Column(name = "media_area", length = 100)
    private String mediaArea;

    /** 掲載枠 */
    @Column(name = "media_slots")
    private Integer mediaSlots;

    /** 掲載年月 */
    @Convert(converter = YearMonthConverter.class)
    @Column(name = "media_year_month", nullable = false, length = 7)
    private YearMonth mediaYearMonth;

    /** コスト */
    @Column(precision = 15, scale = 0)
    private BigDecimal cost;

    /** 採用数 */
    private Integer hires;

    /** 単価 */
    @Column(name = "unit_price", precision = 15, scale = 2)
    private BigDecimal unitPrice;
}