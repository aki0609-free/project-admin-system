package com.project.backend.features.tax.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.employee.entity.Employee;
import com.project.backend.features.employee.repository.EmployeeRepository;
import com.project.backend.features.operation.monthly.enums.MonthlyClosingStatus;
import com.project.backend.features.operation.monthly.repository.MonthlyClosingRepository;
import com.project.backend.features.tax.dto.*;
import com.project.backend.features.tax.entity.*;
import com.project.backend.features.tax.enums.ResidentTaxInputStatus;
import com.project.backend.features.tax.repository.*;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ResidentTaxEditorService {
    private static final List<Integer> FISCAL_MONTHS =
            List.of(6, 7, 8, 9, 10, 11, 12, 1, 2, 3, 4, 5);
    private static final int MAX_TAX_AMOUNT = 10_000_000;

    private final EmployeeRepository employeeRepository;
    private final ResidentTaxMonthlyRepository monthlyRepository;
    private final ResidentTaxInputBatchRepository batchRepository;
    private final ResidentTaxInputRowRepository rowRepository;
    private final MonthlyClosingRepository closingRepository;

    @Transactional(readOnly = true)
    public ResidentTaxEditorResponse findEditor(Integer fiscalYear) {
        validateFiscalYear(fiscalYear);
        ResidentTaxInputBatch batch = batchRepository
                .findFirstByFiscalYearAndSourceTypeAndConfirmedAtIsNullOrderByIdDesc(fiscalYear, "MANUAL")
                .orElse(null);
        return buildResponse(fiscalYear, batch);
    }

    @Transactional
    public ResidentTaxEditorResponse saveDraft(ResidentTaxDraftSaveRequest request) {
        if (request == null) throw new IllegalArgumentException("入力内容は必須です。");
        validateFiscalYear(request.fiscalYear());
        validateInputs(request.employees());

        Map<Long, Employee> employees = employeeRepository
                .findAllByDeletedAtIsNullOrderByIdAsc().stream()
                .collect(Collectors.toMap(Employee::getId, Function.identity()));
        for (ResidentTaxEmployeeInput input : request.employees()) {
            if (!employees.containsKey(input.employeeId())) {
                throw new IllegalArgumentException("存在しない従業員です。employeeId=" + input.employeeId());
            }
        }

        ResidentTaxInputBatch batch = batchRepository
                .findFirstByFiscalYearAndSourceTypeAndConfirmedAtIsNullOrderByIdDesc(
                        request.fiscalYear(), "MANUAL")
                .orElseGet(ResidentTaxInputBatch::new);
        batch.setFiscalYear(request.fiscalYear());
        batch.setSourceType("MANUAL");
        batch.setStatus(ResidentTaxInputStatus.VALIDATED);
        batch.setInputBy(currentUser());
        batch = batchRepository.save(batch);

        rowRepository.deleteByBatchId(batch.getId());
        rowRepository.flush();
        List<ResidentTaxInputRow> rows = new ArrayList<>();
        for (ResidentTaxEmployeeInput employeeInput : request.employees()) {
            for (ResidentTaxMonthInput monthInput : employeeInput.months()) {
                ResidentTaxInputRow row = new ResidentTaxInputRow();
                row.setBatchId(batch.getId());
                row.setEmployeeId(employeeInput.employeeId());
                row.setMonth(monthInput.month());
                row.setTaxAmount(monthInput.taxAmount());
                row.setCurrentTaxAmount(monthlyRepository
                        .findByEmployeeIdAndFiscalYearAndMonth(
                                employeeInput.employeeId(), request.fiscalYear(), monthInput.month())
                        .map(ResidentTaxMonthly::getTaxAmount)
                        .orElse(null));
                rows.add(row);
            }
        }
        rowRepository.saveAll(rows);
        return buildResponse(request.fiscalYear(), batch);
    }

    @Transactional
    public ResidentTaxEditorResponse confirm(Long batchId, ResidentTaxConfirmRequest request) {
        ResidentTaxInputBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("住民税入力の下書きが見つかりません。"));
        if (batch.getStatus() != ResidentTaxInputStatus.VALIDATED || batch.getConfirmedAt() != null) {
            throw new IllegalStateException("検証済みの未確定データだけ確定できます。");
        }
        if (request == null || request.changeReason() == null || request.changeReason().isBlank()) {
            throw new IllegalArgumentException("変更理由は必須です。");
        }

        List<ResidentTaxInputRow> rows = rowRepository
                .findByBatchIdAndDeletedAtIsNullOrderByEmployeeIdAscMonthAsc(batchId);
        boolean closedChanges = rows.stream().anyMatch(row ->
                !Objects.equals(row.getCurrentTaxAmount(), row.getTaxAmount())
                        && isClosed(batch.getFiscalYear(), row.getMonth()));
        if (closedChanges && !request.acknowledgeReclosing()) {
            throw new IllegalStateException("締め済み月の変更が含まれます。再締めが必要であることを確認してください。");
        }

        for (ResidentTaxInputRow row : rows) {
            if (Objects.equals(row.getCurrentTaxAmount(), row.getTaxAmount())) continue;
            monthlyRepository.deleteByEmployeeIdAndFiscalYearAndMonth(
                    row.getEmployeeId(), batch.getFiscalYear(), row.getMonth());
            if (row.getTaxAmount() != null) {
                ResidentTaxMonthly monthly = new ResidentTaxMonthly();
                monthly.setEmployeeId(row.getEmployeeId());
                monthly.setFiscalYear(batch.getFiscalYear());
                monthly.setMonth(row.getMonth());
                monthly.setTaxAmount(row.getTaxAmount());
                monthlyRepository.save(monthly);
            }
        }

        batch.setStatus(ResidentTaxInputStatus.CONFIRMED);
        batch.setConfirmedBy(currentUser());
        batch.setConfirmedAt(Instant.now());
        batch.setChangeReason(request.changeReason().trim());
        batchRepository.save(batch);
        return buildResponse(batch.getFiscalYear(), batch);
    }

    private ResidentTaxEditorResponse buildResponse(Integer fiscalYear, ResidentTaxInputBatch batch) {
        Map<String, ResidentTaxInputRow> draft = batch == null
                ? Map.of()
                : rowRepository.findByBatchIdAndDeletedAtIsNullOrderByEmployeeIdAscMonthAsc(batch.getId())
                        .stream().collect(Collectors.toMap(
                                row -> key(row.getEmployeeId(), row.getMonth()),
                                Function.identity()));
        Map<String, ResidentTaxMonthly> current = monthlyRepository
                .findByFiscalYearOrderByEmployeeIdAscMonthAsc(fiscalYear).stream()
                .collect(Collectors.toMap(
                        row -> key(row.getEmployeeId(), row.getMonth()),
                        Function.identity()));

        boolean[] closedChanges = {false};
        List<ResidentTaxEmployeeEditorResponse> result = employeeRepository
                .findAllByDeletedAtIsNullOrderByIdAsc().stream()
                .map(employee -> {
                    List<ResidentTaxMonthEditorResponse> months = FISCAL_MONTHS.stream()
                            .map(month -> {
                                ResidentTaxMonthly currentRow = current.get(key(employee.getId(), month));
                                ResidentTaxInputRow draftRow = draft.get(key(employee.getId(), month));
                                Integer currentAmount = currentRow == null ? null : currentRow.getTaxAmount();
                                Integer draftAmount = draftRow == null ? currentAmount : draftRow.getTaxAmount();
                                boolean changed = !Objects.equals(currentAmount, draftAmount);
                                boolean closed = isClosed(fiscalYear, month);
                                if (changed && closed) closedChanges[0] = true;
                                return new ResidentTaxMonthEditorResponse(
                                        month, currentAmount, draftAmount, changed, closed);
                            }).toList();
                    return new ResidentTaxEmployeeEditorResponse(
                            employee.getId(), employee.getEmployeeCode(), employee.getEmployeeName(), months);
                }).toList();

        return new ResidentTaxEditorResponse(
                batch == null ? null : batch.getId(),
                fiscalYear,
                batch == null ? "NONE" : batch.getStatus().name(),
                closedChanges[0],
                result);
    }

    private void validateInputs(List<ResidentTaxEmployeeInput> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            throw new IllegalArgumentException("従業員別住民税を1件以上入力してください。");
        }
        Set<Long> employeeIds = new HashSet<>();
        for (ResidentTaxEmployeeInput input : inputs) {
            if (input == null || input.employeeId() == null || !employeeIds.add(input.employeeId())) {
                throw new IllegalArgumentException("従業員IDが未指定または重複しています。");
            }
            if (input.months() == null || input.months().size() != 12) {
                throw new IllegalArgumentException("各従業員は6月から翌年5月まで12か月分が必要です。");
            }
            Set<Integer> months = new HashSet<>();
            for (ResidentTaxMonthInput month : input.months()) {
                if (month == null || month.month() == null || !FISCAL_MONTHS.contains(month.month())
                        || !months.add(month.month())) {
                    throw new IllegalArgumentException("対象月が未指定、不正、または重複しています。");
                }
                if (month.taxAmount() != null
                        && (month.taxAmount() < 0 || month.taxAmount() > MAX_TAX_AMOUNT)) {
                    throw new IllegalArgumentException("住民税額は0円以上10,000,000円以下で入力してください。");
                }
            }
        }
    }

    private void validateFiscalYear(Integer fiscalYear) {
        if (fiscalYear == null || fiscalYear < 2000 || fiscalYear > 2100) {
            throw new IllegalArgumentException("年度は2000から2100の範囲で指定してください。");
        }
    }

    private boolean isClosed(Integer fiscalYear, Integer month) {
        int year = month >= 6 ? fiscalYear : fiscalYear + 1;
        return closingRepository.findByTargetMonthAndDeletedAtIsNull(LocalDate.of(year, month, 1))
                .map(closing -> closing.getStatus() == MonthlyClosingStatus.CLOSED)
                .orElse(false);
    }

    private String key(Long employeeId, Integer month) {
        return employeeId + ":" + month;
    }

    private String currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return "SYSTEM";
        }
        return authentication.getName();
    }
}
