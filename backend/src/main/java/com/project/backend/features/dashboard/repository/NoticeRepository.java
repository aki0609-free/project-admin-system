package com.project.backend.features.dashboard.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.dashboard.entity.Notice;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    List<Notice> findByActiveFlagTrueAndDeletedAtIsNullOrderByPinnedFlagDescStartDateDesc();

    List<Notice> findByActiveFlagTrueAndDeletedAtIsNullAndStartDateBetweenOrderByStartDateAsc(
            LocalDate from,
            LocalDate to
    );

    Optional<Notice> findByIdAndDeletedAtIsNull(Long id);
}