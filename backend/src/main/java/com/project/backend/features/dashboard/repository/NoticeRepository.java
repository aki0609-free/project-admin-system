package com.project.backend.features.dashboard.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.backend.features.dashboard.entity.Notice;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    @Query("""
            SELECT notice
            FROM Notice notice
            WHERE notice.tenantId = :tenantId
              AND notice.activeFlag = true
              AND notice.deletedAt IS NULL
            ORDER BY notice.id DESC
            """)
    List<Notice> findAllActive(
            @Param("tenantId") String tenantId,
            Pageable pageable
    );

    @Query("""
            SELECT notice
            FROM Notice notice
            WHERE notice.tenantId = :tenantId
              AND notice.activeFlag = true
              AND notice.deletedAt IS NULL
              AND notice.startDate <= :to
              AND notice.endDate >= :from
            ORDER BY notice.startDate ASC, notice.pinnedFlag DESC, notice.id ASC
            """)
    List<Notice> findActiveOverlappingPeriod(
            @Param("tenantId") String tenantId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Pageable pageable
    );

    Optional<Notice> findByIdAndTenantIdAndDeletedAtIsNull(
            Long id,
            String tenantId
    );
}
