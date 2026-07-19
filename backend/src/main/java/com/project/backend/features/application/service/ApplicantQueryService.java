package com.project.backend.features.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.application.dto.ApplicantDetailResponse;
import com.project.backend.features.application.dto.ApplicantListItemResponse;
import com.project.backend.features.application.entity.Applicant;
import com.project.backend.features.application.mapper.ApplicantMapper;
import com.project.backend.features.application.repository.ApplicantRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApplicantQueryService {

    private final ApplicantRepository applicantRepository;
    private final ApplicantMapper applicantMapper;

    public List<ApplicantListItemResponse> findAll() {
        return applicantRepository.findAllWithApplicationMedia().stream()
                .map(applicantMapper::toListItem)
                .toList();
    }

    public ApplicantDetailResponse findDetail(Long id) {
        Applicant applicant = applicantRepository.findDetailById(id)
                .orElseThrow(() -> new IllegalArgumentException("応募者が見つかりません。id=" + id));

        return applicantMapper.toDetail(applicant);
    }
}
