package com.project.backend.features.system.rule.service.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.project.backend.features.system.rule.enums.RuleDataType;

class RuleValueConverterTest {

    private final RuleValueConverter converter =
            new RuleValueConverter();

    @Test
    void convert_shouldConvertAllSupportedTypes() {
        assertThat(converter.convert(
                "10",
                RuleDataType.INTEGER,
                "value"
        )).isEqualTo(10);
        assertThat(converter.convert(
                "20",
                RuleDataType.LONG,
                "value"
        )).isEqualTo(20L);
        assertThat(converter.convert(
                "12.50",
                RuleDataType.DECIMAL,
                "value"
        )).isEqualTo(new BigDecimal("12.50"));
        assertThat(converter.convert(
                "true",
                RuleDataType.BOOLEAN,
                "value"
        )).isEqualTo(true);
        assertThat(converter.convert(
                "2026-07-25",
                RuleDataType.DATE,
                "value"
        )).isEqualTo(LocalDate.of(2026, 7, 25));
        assertThat(converter.convert(
                "2026-07-25T09:30:00",
                RuleDataType.DATETIME,
                "value"
        )).isEqualTo(LocalDateTime.of(
                2026,
                7,
                25,
                9,
                30
        ));
    }

    @Test
    void convert_shouldRejectAmbiguousBoolean() {
        assertThatThrownBy(() ->
                converter.convert(
                        "yes",
                        RuleDataType.BOOLEAN,
                        "enabled"
                ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BOOLEAN");
    }
}
