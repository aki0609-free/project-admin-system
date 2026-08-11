package com.project.backend.features.master.payrollitem.transaction;

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
import com.project.backend.features.master.payrollitem.balance.EmployeePayrollItemEnrollmentRepository;
import com.project.backend.features.master.payrollitem.balance.PayrollItemBalancePolicy;
import com.project.backend.features.master.payrollitem.balance.PayrollItemBalancePolicyRepository;
import com.project.backend.features.master.payrollitem.enums.PayrollItemInputSource;
import com.project.backend.features.master.payrollitem.enums.PayrollItemTargetType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmployeePayrollItemTransactionService {

    private static final String COLLECTION_MODE = "collectionMode";
    private static final String MONTHLY = "MONTHLY";

    private final EmployeePayrollItemTransactionRepository repository;
    private final EmployeeRepository employeeRepository;
    private final DeductionMasterRepository deductionMasterRepository;
    private final PayrollItemBalancePolicyRepository policyRepository;
    private final EmployeePayrollItemEnrollmentRepository enrollmentRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<EmployeePayrollItemTransactionResponse> findAll(
            Long employeeId,
            String targetCode,
            String targetMonth
    ) {
        requireEmployee(employeeId);
        requireText(targetCode, "targetCode");
        return repository
                .findAllByTenantIdAndEmployeeIdAndTargetCodeAndTargetMonthAndDeletedAtIsNullOrderByTransactionDateDescIdDesc(
                        TenantContext.getTenantId(), employeeId, targetCode,
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
                employeeId, request.targetCode()
        );
        var master = deductionMasterRepository
                .findByIdAndTenantIdAndDeletedAtIsNull(
                        policy.getTargetMasterId(), TenantContext.getTenantId()
                )
                .filter(item -> Boolean.TRUE.equals(item.getEnabled()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "有効な控除マスターが見つかりません。targetCode="
                                + request.targetCode()
                ));

        EmployeePayrollItemTransaction entity = new EmployeePayrollItemTransaction();
        entity.setEmployeeId(employeeId);
        entity.setTargetType(PayrollItemTargetType.DEDUCTION);
        entity.setTargetMasterId(master.getId());
        entity.setTargetCode(master.getDeductionCode());
        entity.setTargetName(master.getDeductionName());
        apply(entity, request);
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
        if (!entity.getTargetCode().equals(request.targetCode())) {
            throw new IllegalArgumentException("控除項目は変更できません。");
        }
        requireTransactionPolicy(employeeId, request.targetCode());
        apply(entity, request);
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
            EmployeePayrollItemTransactionRequest request
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
        entity.setQuantity(request.quantity() == null
                ? null : request.quantity().setScale(2, RoundingMode.UNNECESSARY));
        entity.setStatus(request.status());
        entity.setSourceReference(blankToNull(request.sourceReference()));
        entity.setNote(blankToNull(request.note()));
    }

    private PayrollItemBalancePolicy requireTransactionPolicy(
            Long employeeId,
            String targetCode
    ) {
        PayrollItemBalancePolicy policy = policyRepository
                .findByTenantIdAndTargetTypeAndTargetCodeAndDeletedAtIsNull(
                        TenantContext.getTenantId(),
                        PayrollItemTargetType.DEDUCTION,
                        targetCode
                )
                .filter(PayrollItemBalancePolicy::isActiveFlag)
                .orElseThrow(() -> new IllegalArgumentException(
                        "従業員別控除ポリシーが見つかりません。targetCode=" + targetCode
                ));
        var enrollment = enrollmentRepository
                .findFirstByEmployeeIdAndBalancePolicyIdAndEffectiveToIsNullAndDeletedAtIsNullOrderByEffectiveFromDesc(
                        employeeId, policy.getId()
                )
                .orElseThrow(() -> new IllegalArgumentException(
                        "従業員に控除項目が適用されていません。targetCode=" + targetCode
                ));
        boolean monthlyOperation = MONTHLY.equalsIgnoreCase(
                readSettings(enrollment.getSettingsJson()).get(COLLECTION_MODE)
        );
        if (policy.getInputSource() != PayrollItemInputSource.TRANSACTION
                && !monthlyOperation) {
            throw new IllegalArgumentException(
                    "この控除項目は日報から入力します。targetCode=" + targetCode
            );
        }
        return policy;
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
            throw new IllegalStateException("従業員別控除設定を読み込めません。", exception);
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
                        "控除取引が見つかりません。id=" + transactionId
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
                entity.getId(), entity.getEmployeeId(), entity.getTargetCode(),
                entity.getTargetName(), YearMonth.from(entity.getTargetMonth()).toString(),
                entity.getTransactionDate(), entity.getAmount(), entity.getQuantity(),
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
