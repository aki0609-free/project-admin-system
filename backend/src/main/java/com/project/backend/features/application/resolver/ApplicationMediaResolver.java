package com.project.backend.features.application.resolver;

import java.time.YearMonth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.application.dto.ApplicationMediaLinkRequest;
import com.project.backend.features.application.entity.ApplicationMedia;
import com.project.backend.features.application.repository.ApplicationMediaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ApplicationMediaResolver {

    private final ApplicationMediaRepository applicationMediaRepository;

    @SuppressWarnings("null")
    public ApplicationMedia resolve(ApplicationMediaLinkRequest request) {
        if (request == null) {
            return null;
        }

        if (request.getId() != null) {
            return applicationMediaRepository.findById(request.getId())
                    .orElseThrow(() -> new IllegalArgumentException("応募媒体が見つかりません。id=" + request.getId()));
        }

        if (request.getMediaName() == null || request.getMediaName().isBlank()) {
            return null;
        }

        if (request.getMediaYearMonth() == null || request.getMediaYearMonth().isBlank()) {
            throw new IllegalArgumentException("応募媒体の掲載年月は必須です。");
        }

        YearMonth yearMonth = YearMonth.parse(request.getMediaYearMonth());

        return applicationMediaRepository.findByMediaNameAndMediaYearMonth(request.getMediaName(), yearMonth)
                .orElseGet(() -> createNew(request, yearMonth));
    }

    @SuppressWarnings("null")
    private ApplicationMedia createNew(ApplicationMediaLinkRequest request, YearMonth yearMonth) {
        ApplicationMedia entity = ApplicationMedia.builder()
                .mediaName(request.getMediaName())
                .mediaArea(request.getMediaArea())
                .mediaSlots(request.getMediaSlots())
                .mediaYearMonth(yearMonth)
                .build();

        return applicationMediaRepository.save(entity);
    }
}