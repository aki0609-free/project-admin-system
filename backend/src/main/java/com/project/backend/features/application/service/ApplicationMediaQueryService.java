package com.project.backend.features.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.application.dto.ApplicationMediaDetailResponse;
import com.project.backend.features.application.dto.ApplicationMediaListItemResponse;
import com.project.backend.features.application.entity.ApplicationMedia;
import com.project.backend.features.application.mapper.ApplicationMediaMapper;
import com.project.backend.features.application.repository.ApplicationMediaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApplicationMediaQueryService {

    private final ApplicationMediaRepository applicationMediaRepository;
    private final ApplicationMediaMapper applicationMediaMapper;

    public List<ApplicationMediaListItemResponse> findAll() {
        return applicationMediaRepository.findAllOrderByYearMonthDesc().stream()
                .map(applicationMediaMapper::toListItem)
                .toList();
    }

    @SuppressWarnings("null")
    public ApplicationMediaDetailResponse findDetail(Long id) {
        ApplicationMedia entity = applicationMediaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("応募媒体が見つかりません。id=" + id));

        return applicationMediaMapper.toDetail(entity);
    }
}