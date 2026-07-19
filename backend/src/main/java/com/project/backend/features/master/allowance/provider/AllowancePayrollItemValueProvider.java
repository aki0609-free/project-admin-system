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

        if (request.targetMasterId() != null) {
            master = repository.findById(request.targetMasterId())
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "手当マスターが見つかりません。id=" + request.targetMasterId()
                            )
                    );

        } else if (StringUtils.hasText(request.targetCode())) {
            master = repository.findByAllowanceCode(request.targetCode())
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
        return switch (queryType) {
            case DAILY -> repository
                    .findByShowOnDailyStatementTrueAndEnabledTrueOrderByDisplayOrderAscIdAsc()
                    .stream()
                    .map(this::toSnapshot)
                    .toList();

            case MONTHLY -> repository
                    .findByShowOnMonthlyStatementTrueAndEnabledTrueOrderByDisplayOrderAscIdAsc()
                    .stream()
                    .map(this::toSnapshot)
                    .toList();

            case PAYROLL, BONUS -> List.of();
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