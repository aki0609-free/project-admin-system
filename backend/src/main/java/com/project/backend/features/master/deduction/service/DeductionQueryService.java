package com.project.backend.features.master.deduction.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.master.deduction.dto.DeductionDetailResponse;
import com.project.backend.features.master.deduction.dto.DeductionListItemResponse;
import com.project.backend.features.master.deduction.entity.DeductionMaster;
import com.project.backend.features.master.deduction.mapper.DeductionMapper;
import com.project.backend.features.master.deduction.repository.DeductionMasterRepository;
import com.project.backend.features.master.deduction.service.resolver.DeductionDetailResolver;
import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.master.payrollitem.balance.PayrollItemPolicyService;
import com.project.backend.features.master.payrollitem.enums.PayrollItemTargetType;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeductionQueryService {

    private final DeductionMasterRepository deductionMasterRepository;
    private final DeductionMapper deductionMapper;
    private final DeductionDetailResolver deductionDetailResolver;
    private final PayrollItemPolicyService policyService;
    private final Clock clock;

    public List<DeductionListItemResponse> findAll() {
        return deductionMasterRepository
                .findByTenantIdAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc(
                        TenantContext.getTenantId()
                )
                .stream()
                .map(deductionMapper::toListItem)
                .toList();
    }

    @SuppressWarnings("null")
    public DeductionDetailResponse findDetail(
            Long id,
            LocalDate targetDate
    ) {
        DeductionMaster deduction = deductionMasterRepository
                .findByIdAndTenantIdAndDeletedAtIsNull(id, TenantContext.getTenantId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "控除マスターが見つかりません。id=" + id
                ));

        LocalDate resolvedTargetDate = targetDate != null
                ? targetDate
                : LocalDate.now(clock);

        return deductionMapper.toDetail(
                deduction,
                deductionDetailResolver.resolve(
                        deduction,
                        resolvedTargetDate
                ),
                policyService.find(PayrollItemTargetType.DEDUCTION, deduction.getId())
        );
    }
}
