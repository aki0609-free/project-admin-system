package com.project.backend.features.operation.monthly.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.operation.monthly.dto.MonthlyClosingPeriod;
import com.project.backend.features.operation.monthly.entity.LegalDepositRefund;
import com.project.backend.features.operation.monthly.enums.LegalDepositRefundStatus;
import com.project.backend.features.operation.monthly.repository.LegalDepositRefundRepository;

class LegalDepositRefundServiceTest {

    private LegalDepositRefundRepository repository;
    private JdbcTemplate jdbcTemplate;
    private LegalDepositRefundService service;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId("default");
        repository = mock(LegalDepositRefundRepository.class);
        jdbcTemplate = mock(JdbcTemplate.class);
        service = new LegalDepositRefundService(
                repository,
                jdbcTemplate,
                Clock.fixed(
                        Instant.parse("2026-08-01T00:00:00Z"),
                        ZoneId.of("Asia/Tokyo")
                )
        );
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void prepareRefunds_shouldSupersedeOldVersionAndCreateLatestBalance() throws Exception {
        LegalDepositRefund previous = new LegalDepositRefund();
        previous.setStatus(LegalDepositRefundStatus.ACTIVE);
        when(repository.findByMonthlyClosingIdAndStatusAndDeletedAtIsNull(
                10L, LegalDepositRefundStatus.ACTIVE
        )).thenReturn(List.of(previous));

        when(jdbcTemplate.query(
                anyString(),
                any(RowMapper.class),
                any(Object[].class)
        )).thenAnswer(invocation -> {
            RowMapper mapper = invocation.getArgument(1);
            ResultSet resultSet = mock(ResultSet.class);
            when(resultSet.getLong("employee_id")).thenReturn(20L);
            when(resultSet.getBigDecimal("balance"))
                    .thenReturn(new BigDecimal("3500.00"));
            return List.of(mapper.mapRow(resultSet, 0));
        });
        when(repository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<LegalDepositRefund> result = service.prepareRefunds(
                10L,
                new MonthlyClosingPeriod(
                        "2026-07",
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2026, 7, 31),
                        null
                ),
                2
        );

        assertThat(previous.getStatus())
                .isEqualTo(LegalDepositRefundStatus.SUPERSEDED);
        assertThat(previous.getSupersededAt())
                .isEqualTo(Instant.parse("2026-08-01T00:00:00Z"));
        assertThat(result).singleElement().satisfies(refund -> {
            assertThat(refund.getMonthlyClosingId()).isEqualTo(10L);
            assertThat(refund.getTargetMonth()).isEqualTo(LocalDate.of(2026, 7, 1));
            assertThat(refund.getPeriodEnd()).isEqualTo(LocalDate.of(2026, 7, 31));
            assertThat(refund.getClosingVersion()).isEqualTo(2);
            assertThat(refund.getEmployeeId()).isEqualTo(20L);
            assertThat(refund.getAmount()).isEqualByComparingTo("3500.00");
            assertThat(refund.getStatus()).isEqualTo(LegalDepositRefundStatus.ACTIVE);
        });
        verify(repository).saveAll(any());
    }
}
