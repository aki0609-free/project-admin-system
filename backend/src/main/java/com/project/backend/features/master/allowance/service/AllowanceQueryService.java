package com.project.backend.features.master.allowance.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.master.allowance.dto.AllowanceDetailResponse;
import com.project.backend.features.master.allowance.dto.AllowanceListItemResponse;
import com.project.backend.features.master.allowance.entity.AllowanceMaster;
import com.project.backend.features.master.allowance.mapper.AllowanceMapper;
import com.project.backend.features.master.allowance.repository.AllowanceMasterRepository;
import com.project.backend.features.master.allowance.service.resolver.AllowanceDetailResolver;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AllowanceQueryService {

    private final AllowanceMasterRepository allowanceMasterRepository;
    private final AllowanceMapper allowanceMapper;
    private final AllowanceDetailResolver allowanceDetailResolver;

    public List<AllowanceListItemResponse> findAll() {
        return allowanceMasterRepository.findAll().stream()
                .map(allowanceMapper::toListItem)
                .toList();
    }

    @SuppressWarnings("null")
    public AllowanceDetailResponse findDetail(Long id) {
        AllowanceMaster allowance = allowanceMasterRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("手当マスターが見つかりません。id=" + id)
                );

        return allowanceMapper.toDetail(
                allowance,
                allowanceDetailResolver.resolve(allowance)
        );
    }
}