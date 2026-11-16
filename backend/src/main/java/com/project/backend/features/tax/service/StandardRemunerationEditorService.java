package com.project.backend.features.tax.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.employee.entity.Employee;
import com.project.backend.features.employee.repository.EmployeeRepository;
import com.project.backend.features.operation.monthly.entity.MonthlyClosing;
import com.project.backend.features.operation.monthly.enums.MonthlyClosingStatus;
import com.project.backend.features.operation.monthly.repository.MonthlyClosingRepository;
import com.project.backend.features.tax.dto.StandardRemunerationConfirmRequest;
import com.project.backend.features.tax.dto.StandardRemunerationDraftSaveRequest;
import com.project.backend.features.tax.dto.StandardRemunerationEditorResponse;
import com.project.backend.features.tax.dto.StandardRemunerationEditorRowResponse;
import com.project.backend.features.tax.dto.StandardRemunerationEmployeeOption;
import com.project.backend.features.tax.dto.StandardRemunerationRowInput;
import com.project.backend.features.tax.entity.EmployeeStandardRemuneration;
import com.project.backend.features.tax.entity.StandardRemunerationInputBatch;
import com.project.backend.features.tax.entity.StandardRemunerationInputRow;
import com.project.backend.features.tax.enums.ResidentTaxInputStatus;
import com.project.backend.features.tax.repository.EmployeeStandardRemunerationRepository;
import com.project.backend.features.tax.repository.StandardRemunerationInputBatchRepository;
import com.project.backend.features.tax.repository.StandardRemunerationInputRowRepository;
import com.project.backend.features.tax.repository.StandardSalaryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StandardRemunerationEditorService {
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("10000000");

    private final EmployeeRepository employeeRepository;
    private final EmployeeStandardRemunerationRepository remunerationRepository;
    private final StandardRemunerationInputBatchRepository batchRepository;
    private final StandardRemunerationInputRowRepository rowRepository;
    private final StandardSalaryRepository standardSalaryRepository;
    private final MonthlyClosingRepository closingRepository;

    @Transactional(readOnly = true)
    public StandardRemunerationEditorResponse findEditor() {
        StandardRemunerationInputBatch batch = batchRepository
                .findFirstByConfirmedAtIsNullAndDeletedAtIsNullOrderByIdDesc()
                .orElse(null);
        return buildResponse(batch);
    }

    @Transactional
    public StandardRemunerationEditorResponse saveDraft(StandardRemunerationDraftSaveRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("入力内容は必須です。");
        }
        List<StandardRemunerationRowInput> inputs = validateInputs(request.rows());

        StandardRemunerationInputBatch batch = batchRepository
                .findFirstByConfirmedAtIsNullAndDeletedAtIsNullOrderByIdDesc()
                .orElseGet(StandardRemunerationInputBatch::new);
        batch.setSourceType("MANUAL");
        batch.setStatus(ResidentTaxInputStatus.VALIDATED);
        batch.setInputBy(currentUser());
        batch = batchRepository.save(batch);

        rowRepository.deleteByBatchId(batch.getId());
        rowRepository.flush();
        List<StandardRemunerationInputRow> rows = new ArrayList<>();
        for (StandardRemunerationRowInput input : inputs) {
            EmployeeStandardRemuneration current = input.currentRecordId() == null
                    ? null
                    : remunerationRepository.findById(input.currentRecordId()).orElseThrow(
                            () -> new IllegalArgumentException("更新対象の標準報酬月額が見つかりません。"));
            if (current != null && !Objects.equals(current.getEmployeeId(), input.employeeId())) {
                throw new IllegalArgumentException("更新対象と従業員が一致しません。");
            }
            StandardRemunerationInputRow row = new StandardRemunerationInputRow();
            row.setBatchId(batch.getId());
            row.setEmployeeId(input.employeeId());
            row.setCurrentRecordId(input.currentRecordId());
            row.setEffectiveFrom(input.effectiveFrom());
            row.setEffectiveTo(input.effectiveTo());
            row.setHealthStandardRemuneration(input.healthStandardRemuneration());
            row.setPensionStandardRemuneration(input.pensionStandardRemuneration());
            row.setSourceType(normalizeSourceType(input.sourceType()));
            row.setNote(trimToNull(input.note()));
            if (current != null) {
                row.setCurrentEffectiveFrom(current.getEffectiveFrom());
                row.setCurrentEffectiveTo(current.getEffectiveTo());
                row.setCurrentHealthStandardRemuneration(current.getHealthStandardRemuneration());
                row.setCurrentPensionStandardRemuneration(current.getPensionStandardRemuneration());
                row.setCurrentSourceType(current.getSourceType());
                row.setCurrentNote(current.getNote());
            }
            rows.add(row);
        }
        rowRepository.saveAll(rows);
        return buildResponse(batch);
    }

    @Transactional
    public StandardRemunerationEditorResponse confirm(
            Long batchId, StandardRemunerationConfirmRequest request) {
        StandardRemunerationInputBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("標準報酬月額の下書きが見つかりません。"));
        if (batch.getStatus() != ResidentTaxInputStatus.VALIDATED || batch.getConfirmedAt() != null) {
            throw new IllegalStateException("検証済みの未確定データだけ確定できます。");
        }
        if (request == null || request.changeReason() == null || request.changeReason().isBlank()) {
            throw new IllegalArgumentException("変更理由は必須です。");
        }
        List<StandardRemunerationInputRow> rows = rowRepository
                .findByBatchIdAndDeletedAtIsNullOrderByEmployeeIdAscEffectiveFromAsc(batchId);
        validateInputs(rows.stream().map(this::toInput).toList());
        boolean closedChanges = rows.stream().anyMatch(row -> isChanged(row) && overlapsClosedPeriod(row));
        if (closedChanges && !request.acknowledgeReclosing()) {
            throw new IllegalStateException("締め済み期間の変更が含まれます。再締めが必要であることを確認してください。");
        }

        for (StandardRemunerationInputRow row : rows) {
            if (!isChanged(row)) continue;
            EmployeeStandardRemuneration target = row.getCurrentRecordId() == null
                    ? new EmployeeStandardRemuneration()
                    : remunerationRepository.findById(row.getCurrentRecordId()).orElseThrow(
                            () -> new IllegalArgumentException("更新対象の標準報酬月額が見つかりません。"));
            target.setEmployeeId(row.getEmployeeId());
            target.setEffectiveFrom(row.getEffectiveFrom());
            target.setEffectiveTo(row.getEffectiveTo());
            target.setHealthStandardRemuneration(row.getHealthStandardRemuneration());
            target.setPensionStandardRemuneration(row.getPensionStandardRemuneration());
            target.setSourceType(row.getSourceType());
            target.setNote(row.getNote());
            remunerationRepository.save(target);
        }

        batch.setStatus(ResidentTaxInputStatus.CONFIRMED);
        batch.setConfirmedBy(currentUser());
        batch.setConfirmedAt(Instant.now());
        batch.setChangeReason(request.changeReason().trim());
        batchRepository.save(batch);
        return buildResponse(batch);
    }

    private List<StandardRemunerationRowInput> validateInputs(List<StandardRemunerationRowInput> raw) {
        if (raw == null || raw.isEmpty()) {
            throw new IllegalArgumentException("従業員別標準報酬月額を1件以上入力してください。");
        }
        Map<Long, Employee> employees = employeeRepository.findAllByDeletedAtIsNullOrderByIdAsc().stream()
                .collect(Collectors.toMap(Employee::getId, Function.identity()));
        Set<Long> currentIds = raw.stream().map(StandardRemunerationRowInput::currentRecordId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        long currentIdCount = raw.stream().map(StandardRemunerationRowInput::currentRecordId)
                .filter(Objects::nonNull).count();
        if (currentIds.size() != currentIdCount) {
            throw new IllegalArgumentException("同じ確定履歴を複数回更新することはできません。");
        }
        Map<Long, List<StandardRemunerationRowInput>> byEmployee = new HashMap<>();
        Set<String> uniqueKeys = new HashSet<>();
        for (StandardRemunerationRowInput input : raw) {
            if (input == null || input.employeeId() == null || !employees.containsKey(input.employeeId())) {
                throw new IllegalArgumentException("存在しない従業員が含まれています。");
            }
            if (input.effectiveFrom() == null
                    || input.effectiveTo() != null && input.effectiveFrom().isAfter(input.effectiveTo())) {
                throw new IllegalArgumentException("適用期間を正しく入力してください。");
            }
            if (!uniqueKeys.add(input.employeeId() + ":" + input.effectiveFrom())) {
                throw new IllegalArgumentException("同じ従業員・適用開始日のデータが重複しています。");
            }
            validateAmount(input.healthStandardRemuneration(), "健康保険用標準報酬月額");
            validateAmount(input.pensionStandardRemuneration(), "厚生年金用標準報酬月額");
            if (input.sourceType() != null && input.sourceType().length() > 30
                    || input.note() != null && input.note().length() > 1000) {
                throw new IllegalArgumentException("決定区分（算定理由）または備考が長すぎます。");
            }
            byEmployee.computeIfAbsent(input.employeeId(), ignored -> new ArrayList<>()).add(input);
        }

        remunerationRepository.findAllByDeletedAtIsNullOrderByEmployeeIdAscEffectiveFromDesc().stream()
                .filter(row -> !currentIds.contains(row.getId()))
                .forEach(row -> byEmployee.computeIfAbsent(row.getEmployeeId(), ignored -> new ArrayList<>())
                        .add(new StandardRemunerationRowInput(
                                row.getId(), row.getEmployeeId(), row.getEffectiveFrom(), row.getEffectiveTo(),
                                row.getHealthStandardRemuneration(), row.getPensionStandardRemuneration(),
                                row.getSourceType(), row.getNote())));
        for (List<StandardRemunerationRowInput> periods : byEmployee.values()) {
            periods.sort(Comparator.comparing(StandardRemunerationRowInput::effectiveFrom));
            for (int i = 1; i < periods.size(); i++) {
                LocalDate previousEnd = periods.get(i - 1).effectiveTo();
                if (previousEnd == null || !previousEnd.isBefore(periods.get(i).effectiveFrom())) {
                    throw new IllegalArgumentException("同一従業員の適用期間が重複しています。");
                }
            }
        }
        return raw;
    }

    private void validateAmount(BigDecimal amount, String label) {
        if (amount == null || amount.signum() < 0 || amount.compareTo(MAX_AMOUNT) > 0
                || amount.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException(label + "は0円以上10,000,000円以下の整数で入力してください。");
        }
        if (!standardSalaryRepository.existsByStandardSalary(amount.intValueExact())) {
            throw new IllegalArgumentException(label + "が標準報酬月額マスターに存在しません。金額=" + amount);
        }
    }

    private StandardRemunerationEditorResponse buildResponse(StandardRemunerationInputBatch batch) {
        Map<Long, Employee> employees = employeeRepository.findAllByDeletedAtIsNullOrderByIdAsc().stream()
                .collect(Collectors.toMap(Employee::getId, Function.identity()));
        List<StandardRemunerationEmployeeOption> options = employees.values().stream()
                .sorted(Comparator.comparing(Employee::getId))
                .map(employee -> new StandardRemunerationEmployeeOption(
                        employee.getId(), employee.getEmployeeCode(), employee.getEmployeeName()))
                .toList();
        List<StandardRemunerationEditorRowResponse> rows;
        if (batch == null || batch.getConfirmedAt() != null) {
            rows = remunerationRepository.findAllByDeletedAtIsNullOrderByEmployeeIdAscEffectiveFromDesc()
                    .stream().map(row -> toResponse(row, employees.get(row.getEmployeeId()))).toList();
        } else {
            rows = rowRepository.findByBatchIdAndDeletedAtIsNullOrderByEmployeeIdAscEffectiveFromAsc(batch.getId())
                    .stream().map(row -> toResponse(row, employees.get(row.getEmployeeId()))).toList();
        }
        boolean hasClosedChanges = rows.stream().anyMatch(row -> row.changed() && row.closed());
        return new StandardRemunerationEditorResponse(
                batch == null ? null : batch.getId(),
                batch == null ? "NONE" : batch.getStatus().name(),
                hasClosedChanges, options, rows);
    }

    private StandardRemunerationEditorRowResponse toResponse(
            EmployeeStandardRemuneration row, Employee employee) {
        return new StandardRemunerationEditorRowResponse(
                row.getId(), row.getEmployeeId(), employeeCode(employee), employeeName(employee),
                row.getEffectiveFrom(), row.getEffectiveTo(), row.getHealthStandardRemuneration(),
                row.getPensionStandardRemuneration(), row.getSourceType(), row.getNote(), false, false);
    }

    private StandardRemunerationEditorRowResponse toResponse(
            StandardRemunerationInputRow row, Employee employee) {
        boolean changed = isChanged(row);
        return new StandardRemunerationEditorRowResponse(
                row.getCurrentRecordId(), row.getEmployeeId(), employeeCode(employee), employeeName(employee),
                row.getEffectiveFrom(), row.getEffectiveTo(), row.getHealthStandardRemuneration(),
                row.getPensionStandardRemuneration(), row.getSourceType(), row.getNote(), changed,
                changed && overlapsClosedPeriod(row));
    }

    private boolean isChanged(StandardRemunerationInputRow row) {
        return row.getCurrentRecordId() == null
                || !Objects.equals(row.getCurrentEffectiveFrom(), row.getEffectiveFrom())
                || !Objects.equals(row.getCurrentEffectiveTo(), row.getEffectiveTo())
                || !sameAmount(row.getCurrentHealthStandardRemuneration(), row.getHealthStandardRemuneration())
                || !sameAmount(row.getCurrentPensionStandardRemuneration(), row.getPensionStandardRemuneration())
                || !Objects.equals(row.getCurrentSourceType(), row.getSourceType())
                || !Objects.equals(row.getCurrentNote(), row.getNote());
    }

    private boolean overlapsClosedPeriod(StandardRemunerationInputRow row) {
        LocalDate rowEnd = row.getEffectiveTo() == null ? LocalDate.of(9999, 12, 31) : row.getEffectiveTo();
        for (MonthlyClosing closing : closingRepository.findAll()) {
            if (closing.getDeletedAt() != null || closing.getStatus() != MonthlyClosingStatus.CLOSED) continue;
            LocalDate start = closing.getClosingStartDate() == null
                    ? closing.getTargetMonth().withDayOfMonth(1) : closing.getClosingStartDate();
            LocalDate end = closing.getClosingEndDate() == null
                    ? closing.getTargetMonth().withDayOfMonth(closing.getTargetMonth().lengthOfMonth())
                    : closing.getClosingEndDate();
            if (!rowEnd.isBefore(start) && !row.getEffectiveFrom().isAfter(end)) return true;
        }
        return false;
    }

    private StandardRemunerationRowInput toInput(StandardRemunerationInputRow row) {
        return new StandardRemunerationRowInput(
                row.getCurrentRecordId(), row.getEmployeeId(), row.getEffectiveFrom(), row.getEffectiveTo(),
                row.getHealthStandardRemuneration(), row.getPensionStandardRemuneration(),
                row.getSourceType(), row.getNote());
    }

    private boolean sameAmount(BigDecimal left, BigDecimal right) {
        return left == null ? right == null : right != null && left.compareTo(right) == 0;
    }

    private String normalizeSourceType(String sourceType) {
        return sourceType == null || sourceType.isBlank() ? "MANUAL" : sourceType.trim();
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String employeeCode(Employee employee) {
        return employee == null ? "" : employee.getEmployeeCode();
    }

    private String employeeName(Employee employee) {
        return employee == null ? "削除済み従業員" : employee.getEmployeeName();
    }

    private String currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null || authentication.getName() == null || authentication.getName().isBlank()
                ? "SYSTEM" : authentication.getName();
    }
}
