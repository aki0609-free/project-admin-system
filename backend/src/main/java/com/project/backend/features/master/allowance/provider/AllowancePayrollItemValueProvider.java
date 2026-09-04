package com.project.backend.features.master.allowance.provider;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.project.backend.features.master.allowance.entity.AllowanceMaster;
import com.project.backend.features.master.allowance.repository.AllowanceMasterRepository;
import com.project.backend.features.master.payrollitem.dto.PayrollItemMasterSnapshot;
import com.project.backend.features.master.payrollitem.dto.PayrollItemValueRequest;
import com.project.backend.features.master.payrollitem.enums.PayrollItemQueryType;
import com.project.backend.features.master.payrollitem.enums.PayrollItemTargetType;
import com.project.backend.features.master.payrollitem.provider.PayrollItemValueProvider;
import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.master.allowance.enums.AllowanceUnit;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AllowancePayrollItemValueProvider implements PayrollItemValueProvider {

    private final AllowanceMasterRepository repository;

    @Override
    public PayrollItemTargetType supports() {
        return PayrollItemTargetType.ALLOWANCE;
    }

    @SuppressWarnings("null")
    @Override
    public PayrollItemMasterSnapshot findMaster(
            PayrollItemValueRequest request
    ) {
        AllowanceMaster master;
        String tenantId = TenantContext.getTenantId();

        if (request.targetMasterId() != null) {
            master = repository.findByIdAndTenantIdAndDeletedAtIsNull(
                            request.targetMasterId(),
                            tenantId
                    )
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "手当マスターが見つかりません。id=" + request.targetMasterId()
                            )
                    );

        } else if (StringUtils.hasText(request.targetCode())) {
            master = repository.findByTenantIdAndAllowanceCodeAndDeletedAtIsNull(
                            tenantId,
                            request.targetCode()
                    )
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "手当マスターが見つかりません。code=" + request.targetCode()
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
                    .findByTenantIdAndAllowanceUnitInAndShowOnDailyStatementTrueAndEnabledTrueAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc(
                            tenantId,
                            List.of(AllowanceUnit.DAILY, AllowanceUnit.BOTH)
                    )
                    .stream()
                    .map(this::toSnapshot)
                    .toList();

            // 月次給与はSQL Viewで集計・確定する。汎用Rule候補は返さない。
            case MONTHLY, PAYROLL, BONUS -> List.of();
        };
    }

    private PayrollItemMasterSnapshot toSnapshot(
            AllowanceMaster master
    ) {
        return new PayrollItemMasterSnapshot(
                PayrollItemTargetType.ALLOWANCE,
                master.getId(),
                master.getAllowanceCode(),
                master.getAllowanceName(),
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
