package com.project.backend.features.application.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.YearMonth;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import com.project.backend.features.application.dto.ApplicationMediaUpdateRequest;
import com.project.backend.features.application.entity.ApplicationMedia;

class ApplicationMediaMapperTest {

    private final ApplicationMediaMapper mapper = Mappers.getMapper(ApplicationMediaMapper.class);

    @Test
    void updateClearsNullableInputAndKeepsCalculatedFields() {
        ApplicationMedia entity = ApplicationMedia.builder()
                .mediaName("媒体A")
                .mediaArea("東京")
                .mediaSlots(2)
                .mediaYearMonth(YearMonth.of(2026, 8))
                .cost(BigDecimal.valueOf(10000))
                .hires(2)
                .unitPrice(BigDecimal.valueOf(5000))
                .build();

        mapper.update(
                entity,
                new ApplicationMediaUpdateRequest(
                        "媒体A",
                        null,
                        null,
                        "2026-08",
                        null
                )
        );

        assertThat(entity.getMediaArea()).isNull();
        assertThat(entity.getMediaSlots()).isNull();
        assertThat(entity.getCost()).isNull();
        assertThat(entity.getHires()).isEqualTo(2);
        assertThat(entity.getUnitPrice()).isEqualByComparingTo("5000");
        assertThat(entity.getMediaYearMonth()).isEqualTo(YearMonth.of(2026, 8));
    }
}
