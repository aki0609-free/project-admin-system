package com.project.backend.features.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.application.repository.ApplicantRepository;
import com.project.backend.features.application.entity.ApplicationMedia;
import com.project.backend.features.application.repository.ApplicationMediaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ApplicationMediaMetricsService {

    private final ApplicationMediaRepository applicationMediaRepository;
    private final ApplicantRepository applicantRepository;

    public void recalculate(Long applicationMediaId) {
        if (applicationMediaId == null) {
            return;
        }

        ApplicationMedia media = applicationMediaRepository.findById(applicationMediaId)
                .orElseThrow(() -> new IllegalArgumentException("応募媒体が見つかりません。id=" + applicationMediaId));

        long hires = applicantRepository.countHiredByApplicationMediaId(applicationMediaId);

        media.setHires((int) hires);

        if (media.getCost() != null && hires > 0) {
            media.setUnitPrice(
                    media.getCost().divide(BigDecimal.valueOf(hires), 2, RoundingMode.HALF_UP)
            );
        } else {
            media.setUnitPrice(null);
        }

        applicationMediaRepository.save(media);
    }
}