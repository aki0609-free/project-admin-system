package com.project.backend.features.master.payrollitem.transaction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.employee.repository.EmployeeRepository;
import com.project.backend.features.master.deduction.repository.DeductionMasterRepository;
import com.project.backend.features.master.allowance.repository.AllowanceMasterRepository;
import com.project.backend.features.master.payrollitem.balance.EmployeePayrollItemEnrollmentRepository;
import com.project.backend.features.master.payrollitem.balance.PayrollItemBalancePolicy;
import com.project.backend.features.master.payrollitem.balance.PayrollItemBalancePolicyRepository;
import com.project.backend.features.master.payrollitem.balance.PayrollItemBalanceQueryService;
import com.project.backend.features.master.payrollitem.balance.PayrollItemParameterDefinition;
import com.project.backend.features.master.payrollitem.balance.PayrollItemParameterDefinitionRepository;
import com.project.backend.features.master.payrollitem.enums.PayrollItemInputSource;
import com.project.backend.features.master.payrollitem.enums.PayrollItemTargetType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmployeePayrollItemTransactionService {

    private final EmployeePayrollItemTransactionRepository repository;
    private final EmployeeRepository employeeRepository;
    private final DeductionMasterRepository deductionMasterRepository;
    private final AllowanceMasterRepository allowanceMasterRepository;
    private final PayrollItemBalancePolicyRepository policyRepository;
    private final EmployeePayrollItemEnrollmentRepository enrollmentRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final PayrollItemParameterDefinitionRepository parameterDefinitionRepository;
    private final PayrollItemBalanceQueryService balanceQueryService;

    @Transactional(readOnly = true)
    public List<EmployeePayrollItemTransactionResponse> findAll(
            Long employeeId,
            PayrollItemTargetType targetType,
            String targetCode,
            String targetMonth
    ) {
        requireEmployee(employeeId);
        requireText(targetCode, "targetCode");
        return repository
                .findAllByTenantIdAndEmployeeIdAndTargetTypeAndTargetCodeAndTargetMonthAndDeletedAtIsNullOrderByTransactionDateDescIdDesc(
                        TenantContext.getTenantId(), employeeId, targetType, targetCode,
                        YearMonth.parse(targetMonth).atDay(1)
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public EmployeePayrollItemTransactionResponse create(
            Long employeeId,
            EmployeePayrollItemTransactionRequest request
    ) {
        requireEmployee(employeeId);
        PayrollItemBalancePolicy policy = requireTransactionPolicy(
                employeeId, request.targetType(), request.targetCode()
        );

        EmployeePayrollItemTransaction entity = new EmployeePayrollItemTransaction();
        entity.setEmployeeId(employeeId);
        entity.setTargetType(request.targetType());
        entity.setTargetMasterId(policy.getTargetMasterId());
        entity.setTargetCode(policy.getTargetCode());
        entity.setTargetName(requireEnabledMasterName(policy));
        apply(entity, request, policy);
        entity.setSourceType(PayrollItemTransactionSource.MANUAL);
        return toResponse(repository.save(entity));
    }

    @Transactional
    public EmployeePayrollItemTransactionResponse update(
            Long employeeId,
            Long transactionId,
            EmployeePayrollItemTransactionRequest request
    ) {
        requireEmployee(employeeId);
        EmployeePayrollItemTransaction entity = requireOwned(
                employeeId, transactionId
        );
        if (entity.getTargetType() != request.targetType()
                || !entity.getTargetCode().equals(request.targetCode())) {
            throw new IllegalArgumentException("手当・控除項目は変更できません。");
        }
        PayrollItemBalancePolicy policy = requireTransactionPolicy(
                employeeId, request.targetType(), request.targetCode());
        apply(entity, request, policy);
        return toResponse(entity);
    }

    @Transactional
    public void delete(Long employeeId, Long transactionId) {
        requireEmployee(employeeId);
        EmployeePayrollItemTransaction entity = requireOwned(
                employeeId, transactionId
        );
        entity.setDeletedAt(Instant.now(clock));
    }

    private void apply(
            EmployeePayrollItemTransaction entity,
            EmployeePayrollItemTransactionRequest request,
            PayrollItemBalancePolicy policy
    ) {
        YearMonth targetMonth = YearMonth.parse(request.targetMonth());
        LocalDate transactionDate = request.transactionDate();
        if (!YearMonth.from(transactionDate).equals(targetMonth)) {
            throw new IllegalArgumentException(
                    "明細日は対象月の範囲内で指定してください。"
            );
        }
        entity.setTargetMonth(targetMonth.atDay(1));
        entity.setTransactionDate(transactionDate);
        entity.setAmount(request.amount().setScale(2, RoundingMode.UNNECESSARY));
        PayrollItemTransactionPurpose purpose = request.transactionPurpose() == null
                ? PayrollItemTransactionPurpose.PAYROLL_ITEM
                : request.transactionPurpose();
        entity.setTransactionPurpose(purpose);
        BigDecimal quantity = request.quantity();
        if (policy.isBalanceTrackingFlag()
                && policy.getBalanceUnit()
                == com.project.backend.features.master.payrollitem.balance.BalanceUnit.AMOUNT) {
            quantity = request.amount();
        }
        entity.setQuantity(quantity == null
                ? null : quantity.setScale(2, RoundingMode.UNNECESSARY));
        PayrollItemBalanceEffect effect = request.balanceEffect() == null
                ? (policy.isBalanceTrackingFlag()
                ? PayrollItemBalanceEffect.DEBIT
                : PayrollItemBalanceEffect.NONE)
                : request.balanceEffect();
        if (purpose == PayrollItemTransactionPurpose.BALANCE_ACCRUAL) {
            if (!policy.isBalanceTrackingFlag()) {
                throw new IllegalArgumentException(
                        "請求・残高発生は残高管理する項目だけ登録できます。"
                );
            }
            effect = PayrollItemBalanceEffect.CREDIT;
        }
        if (!policy.isBalanceTrackingFlag()
                && effect != PayrollItemBalanceEffect.NONE) {
            throw new IllegalArgumentException(
                    "残高管理しない項目は残高を増減できません。"
            );
        }
        if (effect != PayrollItemBalanceEffect.NONE
                && entity.getQuantity() == null) {
            throw new IllegalArgumentException(
                    "残高を増減する取引では数量が必須です。"
            );
        }
        validateConfirmedConsumption(entity, request, policy, effect);
        entity.setBalanceEffect(effect);
        entity.setStatus(request.status());
        entity.setSourceReference(blankToNull(request.sourceReference()));
        entity.setNote(blankToNull(request.note()));
    }

    private void validateConfirmedConsumption(
            EmployeePayrollItemTransaction entity,
            EmployeePayrollItemTransactionRequest request,
            PayrollItemBalancePolicy policy,
            PayrollItemBalanceEffect effect
    ) {
        if (!policy.isBalanceTrackingFlag()
                || policy.isAdvanceConsumptionFlag()
                || effect != PayrollItemBalanceEffect.DEBIT
                || request.status() != PayrollItemTransactionStatus.CONFIRMED) {
            return;
        }
        var balance = request.targetType() == PayrollItemTargetType.ALLOWANCE
                ? balanceQueryService.findAllowanceBalance(
                        entity.getEmployeeId(), policy.getTargetMasterId(),
                        request.transactionDate(), null, entity.getId())
                : balanceQueryService.findDeductionBalance(
                        entity.getEmployeeId(), policy.getTargetMasterId(),
                        request.transactionDate(), null, entity.getId());
        if (entity.getQuantity().compareTo(balance.remainingQuantity()) > 0) {
            throw new IllegalArgumentException(
                    policy.getDisplayName() + "の消化数量が残数量を超えています。remaining="
                            + balance.remainingQuantity() + ", quantity="
                            + entity.getQuantity()
            );
        }
    }

    private PayrollItemBalancePolicy requireTransactionPolicy(
            Long employeeId,
            PayrollItemTargetType targetType,
            String targetCode
    ) {
        PayrollItemBalancePolicy policy = policyRepository
                .findByTenantIdAndTargetTypeAndTargetCodeAndDeletedAtIsNull(
                        TenantContext.getTenantId(),
                        targetType,
                        targetCode
                )
                .filter(PayrollItemBalancePolicy::isActiveFlag)
                .orElseThrow(() -> new IllegalArgumentException(
                        "従業員別手当・控除ポリシーが見つかりません。targetCode=" + targetCode
                ));
        var enrollment = enrollmentRepository
                .findFirstByEmployeeIdAndBalancePolicyIdAndEffectiveToIsNullAndDeletedAtIsNullOrderByEffectiveFromDesc(
                        employeeId, policy.getId()
                )
                .orElseThrow(() -> new IllegalArgumentException(
                        "従業員に手当・控除項目が適用されていません。targetCode=" + targetCode
                ));
        PayrollItemInputSource effectiveInputSource = parameterDefinitionRepository
                .findAllByTenantIdAndBalancePolicyIdAndActiveFlagTrueAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc(
                        TenantContext.getTenantId(), policy.getId())
                .stream()
                .filter(PayrollItemParameterDefinition::isInputSourceOverrideFlag)
                .findFirst()
                .map(definition -> readSettings(enrollment.getSettingsJson())
                        .get(definition.getParameterKey()))
                .filter(value -> value != null && !value.isBlank())
                .map(PayrollItemInputSource::valueOf)
                .orElse(policy.getInputSource());
        if (!effectiveInputSource.supportsTransaction()) {
            throw new IllegalArgumentException(
                    "この手当・控除項目は日報から入力します。targetCode=" + targetCode
            );
        }
        return policy;
    }

    private String requireEnabledMasterName(PayrollItemBalancePolicy policy) {
        String tenantId = TenantContext.getTenantId();
        if (policy.getTargetType() == PayrollItemTargetType.ALLOWANCE) {
            return allowanceMasterRepository
                    .findByIdAndTenantIdAndDeletedAtIsNull(
                            policy.getTargetMasterId(), tenantId)
                    .filter(item -> Boolean.TRUE.equals(item.getEnabled()))
                    .map(item -> item.getAllowanceName())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "有効な手当マスターが見つかりません。targetCode="
                                    + policy.getTargetCode()));
        }
        return deductionMasterRepository
                .findByIdAndTenantIdAndDeletedAtIsNull(
                        policy.getTargetMasterId(), tenantId)
                .filter(item -> Boolean.TRUE.equals(item.getEnabled()))
                .map(item -> item.getDeductionName())
                .orElseThrow(() -> new IllegalArgumentException(
                        "有効な控除マスターが見つかりません。targetCode="
                                + policy.getTargetCode()));
    }

    private Map<String, String> readSettings(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(
                    json, new TypeReference<Map<String, String>>() { }
            );
        } catch (Exception exception) {
            throw new IllegalStateException("従業員別手当・控除設定を読み込めません。", exception);
        }
    }

    private EmployeePayrollItemTransaction requireOwned(
            Long employeeId,
            Long transactionId
    ) {
        return repository
                .findByIdAndTenantIdAndEmployeeIdAndDeletedAtIsNull(
                        transactionId, TenantContext.getTenantId(), employeeId
                )
                .orElseThrow(() -> new IllegalArgumentException(
                        "手当・控除取引が見つかりません。id=" + transactionId
                ));
    }

    private void requireEmployee(Long employeeId) {
        var employee = employeeRepository.findByIdAndDeletedAtIsNull(employeeId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "従業員が見つかりません。employeeId=" + employeeId
                ));
        if (!TenantContext.getTenantId().equals(employee.getTenantId())) {
            throw new IllegalArgumentException(
                    "従業員が見つかりません。employeeId=" + employeeId
            );
        }
    }

    private EmployeePayrollItemTransactionResponse toResponse(
            EmployeePayrollItemTransaction entity
    ) {
        return new EmployeePayrollItemTransactionResponse(
                entity.getId(), entity.getEmployeeId(), entity.getTargetType(),
                entity.getTargetCode(),
                entity.getTargetName(), YearMonth.from(entity.getTargetMonth()).toString(),
                entity.getTransactionDate(), entity.getAmount(), entity.getQuantity(),
                entity.getTransactionPurpose(),
                entity.getBalanceEffect(),
                entity.getSourceType(), entity.getSourceReference(), entity.getStatus(),
                entity.getNote(), entity.getLockVersion()
        );
    }

    private void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " は必須です。");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
