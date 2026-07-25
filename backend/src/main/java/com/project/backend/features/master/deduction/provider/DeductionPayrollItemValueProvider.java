package com.project.backend.features.master.deduction.provider;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.project.backend.features.master.deduction.entity.DeductionMaster;
import com.project.backend.features.master.deduction.repository.DeductionMasterRepository;
import com.project.backend.features.master.payrollitem.dto.PayrollItemMasterSnapshot;
import com.project.backend.features.master.payrollitem.dto.PayrollItemValueRequest;
import com.project.backend.features.master.payrollitem.enums.PayrollItemQueryType;
import com.project.backend.features.master.payrollitem.enums.PayrollItemTargetType;
import com.project.backend.features.master.payrollitem.provider.PayrollItemValueProvider;
import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.master.deduction.enums.DeductionUnit;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DeductionPayrollItemValueProvider implements PayrollItemValueProvider {

    private final DeductionMasterRepository repository;

    @Override
    public PayrollItemTargetType supports() {
        return PayrollItemTargetType.DEDUCTION;
    }

    @SuppressWarnings("null")
    @Override
    public PayrollItemMasterSnapshot findMaster(
            PayrollItemValueRequest request
    ) {
        DeductionMaster master;
        String tenantId = TenantContext.getTenantId();

        if (request.targetMasterId() != null) {
            master = repository.findByIdAndTenantIdAndDeletedAtIsNull(
                            request.targetMasterId(),
                            tenantId
                    )
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "控除マスターが見つかりません。id=" + request.targetMasterId()
                            )
                    );

        } else if (StringUtils.hasText(request.targetCode())) {
            master = repository.findByTenantIdAndDeductionCodeAndDeletedAtIsNull(
                            tenantId,
                            request.targetCode()
                    )
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "控除マスターが見つかりません。code=" + request.targetCode()
                            )
                    );

        } else {
            throw new IllegalArgumentException("targetMasterId または targetCode は必須です。");
        }

        return toSnapshot(master);
    }

    @Override
    public List<PayrollItemMasterSnapshot> findItems(
            PayrollItemQueryType queryType
    ) {
        String tenantId = TenantContext.getTenantId();
        return switch (queryType) {
            case DAILY -> repository
                    .findByTenantIdAndDeductionUnitInAndShowOnDailyStatementTrueAndEnabledTrueAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc(
                            tenantId,
                            List.of(DeductionUnit.DAILY, DeductionUnit.BOTH)
                    )
                    .stream()
                    .map(this::toSnapshot)
                    .toList();

            case MONTHLY -> repository
                    .findByTenantIdAndShowOnMonthlyStatementTrueAndEnabledTrueAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc(
                            tenantId
                    )
                    .stream()
                    .map(this::toSnapshot)
                    .toList();

            case PAYROLL, BONUS -> List.of();
        };
    }

    private PayrollItemMasterSnapshot toSnapshot(
            DeductionMaster master
    ) {
        return new PayrollItemMasterSnapshot(
                PayrollItemTargetType.DEDUCTION,
                master.getId(),
                master.getDeductionCode(),
                master.getDeductionName(),
                master.getCalculationType() == null
                        ? "MANUAL"
                        : master.getCalculationType().name(),
                master.getRuleName(),
                master.getDefaultAmount(),
                master.getMinAmount(),
                master.getMaxAmount(),
                master.getAllowManualInput(),
                master.getDisplayOrder()
        );
    }
}
