package com.project.backend.features.application.repository;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.project.backend.features.application.entity.ApplicationMedia;

public interface ApplicationMediaRepository extends JpaRepository<ApplicationMedia, Long> {

    Optional<ApplicationMedia> findByMediaNameAndMediaYearMonth(String mediaName, YearMonth mediaYearMonth);

    @Query("""
        select am
        from ApplicationMedia am
        order by am.mediaYearMonth desc, am.id desc
    """)
    List<ApplicationMedia> findAllOrderByYearMonthDesc();

    boolean existsByMediaNameAndMediaYearMonth(String mediaName, YearMonth mediaYearMonth);
}
