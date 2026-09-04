package com.project.backend.features.operation.preparation.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.customer.entity.Customer;
import com.project.backend.features.customer.entity.CustomerSite;
import com.project.backend.features.customer.repository.CustomerRepository;
import com.project.backend.features.customer.repository.CustomerSiteRepository;
import com.project.backend.features.employee.entity.Employee;
import com.project.backend.features.employee.repository.EmployeeRepository;
import com.project.backend.features.operation.preparation.dto.DailyPreparationAssignmentBulkSaveItemRequest;
import com.project.backend.features.operation.preparation.dto.DailyPreparationAssignmentBulkSaveRequest;
import com.project.backend.features.operation.preparation.dto.DailyPreparationAssignmentResponse;
import com.project.backend.features.operation.preparation.dto.DailyPreparationAssignmentSaveRequest;
import com.project.backend.features.operation.preparation.dto.DailyPreparationCreateRequest;
import com.project.backend.features.operation.preparation.dto.DailyPreparationDispatchBulkSaveItemRequest;
import com.project.backend.features.operation.preparation.dto.DailyPreparationDispatchBulkSaveRequest;
import com.project.backend.features.operation.preparation.dto.DailyPreparationDispatchResponse;
import com.project.backend.features.operation.preparation.dto.DailyPreparationDispatchSaveRequest;
import com.project.backend.features.operation.preparation.dto.DailyPreparationResponse;
import com.project.backend.features.operation.preparation.entity.DailyPreparation;
import com.project.backend.features.operation.preparation.entity.DailyPreparationAssignment;
import com.project.backend.features.operation.preparation.entity.DailyPreparationDispatch;
import com.project.backend.features.operation.preparation.enums.DailyPreparationStatus;
import com.project.backend.features.operation.preparation.mapper.DailyPreparationMapper;
import com.project.backend.features.operation.preparation.repository.DailyPreparationAssignmentRepository;
import com.project.backend.features.operation.preparation.repository.DailyPreparationDispatchRepository;
import com.project.backend.features.operation.preparation.repository.DailyPreparationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DailyPreparationService {

    private final DailyPreparationRepository preparationRepository;
    private final DailyPreparationAssignmentRepository assignmentRepository;
    private final DailyPreparationDispatchRepository dispatchRepository;

    private final EmployeeRepository employeeRepository;
    private final CustomerRepository customerRepository;
    private final CustomerSiteRepository customerSiteRepository;

    private final DailyPreparationMapper mapper;
    private final Clock clock;

    @Transactional(readOnly = true)
    public DailyPreparationResponse findByTargetDate(LocalDate targetDate) {
        if (targetDate == null) {
            throw new IllegalArgumentException("targetDate は必須です。");
        }

        DailyPreparation preparation = preparationRepository
                .findByTargetDateAndDeletedAtIsNull(targetDate)
                .orElse(null);

        if (preparation == null) {
            return null;
        }

        return toResponse(preparation);
    }

    public DailyPreparationResponse create(DailyPreparationCreateRequest request) {
        if (request == null || request.getTargetDate() == null) {
            throw new IllegalArgumentException("targetDate は必須です。");
        }

        if (preparationRepository.existsByTargetDateAndDeletedAtIsNull(request.getTargetDate())) {
            throw new IllegalArgumentException("指定日の翌日準備は既に存在します。");
        }

        DailyPreparation entity = new DailyPreparation();
        entity.setTargetDate(request.getTargetDate());
        entity.setStatus(DailyPreparationStatus.OPEN);
        entity.setNote(normalizeNote(request.getNote()));

        return toResponse(preparationRepository.save(entity));
    }

    public DailyPreparationResponse updateNote(Long id, String note) {
        DailyPreparation preparation = findPreparation(id);
        preparation.setNote(normalizeNote(note));
        return toResponse(preparationRepository.save(preparation));
    }

    public DailyPreparationAssignmentResponse createAssignment(
            DailyPreparationAssignmentSaveRequest request) {
        validateAssignmentRequest(request);

        DailyPreparation preparation = findPreparation(request.getPreparationId());
        Employee employee = findEmployee(request.getEmployeeId());

        if (assignmentRepository.existsByPreparationIdAndEmployeeIdAndDeletedAtIsNull(
                preparation.getId(),
                employee.getId())) {
            throw new IllegalArgumentException("この従業員の配置は既に存在します。");
        }

        DailyPreparationAssignment entity = new DailyPreparationAssignment();

        applyAssignment(request, entity, preparation, employee);

        return mapper.toAssignmentResponse(assignmentRepository.save(entity));
    }

    @SuppressWarnings("null")
    public DailyPreparationAssignmentResponse updateAssignment(
            Long id,
            DailyPreparationAssignmentSaveRequest request) {
        validateAssignmentRequest(request);

        DailyPreparation preparation = findPreparation(request.getPreparationId());
        Employee employee = findEmployee(request.getEmployeeId());

        DailyPreparationAssignment entity = assignmentRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new IllegalArgumentException("従業員配置が見つかりません。 id=" + id));
        requireSamePreparation(entity.getPreparationId(), preparation.getId());
        if (assignmentRepository.existsByPreparationIdAndEmployeeIdAndIdNotAndDeletedAtIsNull(
                preparation.getId(),
                employee.getId(),
                entity.getId()
        )) {
            throw new IllegalArgumentException("この従業員の配置は既に存在します。");
        }

        applyAssignment(request, entity, preparation, employee);

        return mapper.toAssignmentResponse(assignmentRepository.save(entity));
    }

    public void deleteAssignment(Long id) {
        DailyPreparationAssignment entity = assignmentRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new IllegalArgumentException("従業員配置が見つかりません。 id=" + id));

        entity.setDeletedAt(Instant.now(clock));
    }

    public DailyPreparationDispatchResponse createDispatch(
            DailyPreparationDispatchSaveRequest request) {
        validateDispatchRequest(request);

        DailyPreparation preparation = findPreparation(request.getPreparationId());
        CustomerSite site = findSite(request.getCustomerSiteId());

        if (dispatchRepository.existsByPreparationIdAndCustomerSiteIdAndDeletedAtIsNull(
                preparation.getId(),
                site.getId())) {
            throw new IllegalArgumentException("この現場の配車は既に存在します。");
        }

        DailyPreparationDispatch entity = new DailyPreparationDispatch();

        applyDispatch(request, entity, preparation, site);

        return mapper.toDispatchResponse(dispatchRepository.save(entity));
    }

    @SuppressWarnings("null")
    public DailyPreparationDispatchResponse updateDispatch(
            Long id,
            DailyPreparationDispatchSaveRequest request) {
        validateDispatchRequest(request);

        DailyPreparation preparation = findPreparation(request.getPreparationId());
        CustomerSite site = findSite(request.getCustomerSiteId());

        DailyPreparationDispatch entity = dispatchRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new IllegalArgumentException("現場配車が見つかりません。 id=" + id));
        requireSamePreparation(entity.getPreparationId(), preparation.getId());
        if (dispatchRepository.existsByPreparationIdAndCustomerSiteIdAndIdNotAndDeletedAtIsNull(
                preparation.getId(),
                site.getId(),
                entity.getId()
        )) {
            throw new IllegalArgumentException("この現場の配車は既に存在します。");
        }

        applyDispatch(request, entity, preparation, site);

        return mapper.toDispatchResponse(dispatchRepository.save(entity));
    }

    public void deleteDispatch(Long id) {
        DailyPreparationDispatch entity = dispatchRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new IllegalArgumentException("現場配車が見つかりません。 id=" + id));

        entity.setDeletedAt(Instant.now(clock));
    }

    public DailyPreparationResponse bulkSaveAssignments(
            DailyPreparationAssignmentBulkSaveRequest request) {
        if (request == null || request.getPreparationId() == null) {
            throw new IllegalArgumentException("preparationId は必須です。");
        }

        DailyPreparation preparation = findPreparation(request.getPreparationId());

        for (DailyPreparationAssignmentBulkSaveItemRequest item : safeItems(request.getItems())) {
            if (item == null) {
                throw new IllegalArgumentException("一括保存の行は必須です。");
            }
            validateBulkFlags(item.isNew(), item.isUpdated(), item.isDeleted(), item.getId());
            if (item.isDeleted()) {
                deleteAssignment(item.getId(), preparation.getId());
                continue;
            }

            if (item.isNew()) {
                DailyPreparationAssignmentSaveRequest saveRequest = toAssignmentSaveRequest(preparation.getId(), item);

                createAssignment(saveRequest);
                continue;
            }

            if (item.isUpdated()) {
                DailyPreparationAssignmentSaveRequest saveRequest = toAssignmentSaveRequest(preparation.getId(), item);

                updateAssignment(item.getId(), saveRequest);
            }
        }

        removeUnusedDispatches(preparation.getId());

        return toResponse(preparation);
    }

    private DailyPreparationAssignmentSaveRequest toAssignmentSaveRequest(
            Long preparationId,
            DailyPreparationAssignmentBulkSaveItemRequest item) {
        DailyPreparationAssignmentSaveRequest request = new DailyPreparationAssignmentSaveRequest();

        request.setPreparationId(preparationId);
        request.setEmployeeId(item.getEmployeeId());
        request.setCustomerId(item.getCustomerId());
        request.setCustomerSiteId(item.getCustomerSiteId());
        request.setWorkDescription(item.getWorkDescription());

        return request;
    }

    public DailyPreparationResponse bulkSaveDispatches(
            DailyPreparationDispatchBulkSaveRequest request) {
        if (request == null || request.getPreparationId() == null) {
            throw new IllegalArgumentException("preparationId は必須です。");
        }

        DailyPreparation preparation = findPreparation(request.getPreparationId());

        for (DailyPreparationDispatchBulkSaveItemRequest item : safeItems(request.getItems())) {
            if (item == null) {
                throw new IllegalArgumentException("一括保存の行は必須です。");
            }
            validateBulkFlags(item.isNew(), item.isUpdated(), item.isDeleted(), item.getId());
            if (item.isDeleted()) {
                deleteDispatch(item.getId(), preparation.getId());
                continue;
            }

            if (item.isNew()) {
                DailyPreparationDispatchSaveRequest saveRequest = toDispatchSaveRequest(preparation.getId(), item);

                createDispatch(saveRequest);
                continue;
            }

            if (item.isUpdated()) {
                DailyPreparationDispatchSaveRequest saveRequest = toDispatchSaveRequest(preparation.getId(), item);

                updateDispatch(item.getId(), saveRequest);
            }
        }

        return toResponse(preparation);
    }

    private DailyPreparationDispatchSaveRequest toDispatchSaveRequest(
            Long preparationId,
            DailyPreparationDispatchBulkSaveItemRequest item) {
        DailyPreparationDispatchSaveRequest request = new DailyPreparationDispatchSaveRequest();

        request.setPreparationId(preparationId);
        request.setCustomerId(item.getCustomerId());
        request.setCustomerSiteId(item.getCustomerSiteId());
        request.setVehicleCount(item.getVehicleCount());
        request.setNote(item.getNote());

        return request;
    }

    private DailyPreparationResponse toResponse(DailyPreparation preparation) {
        return mapper.toResponse(
                preparation,
                assignmentRepository.findByPreparationIdAndDeletedAtIsNullOrderByEmployeeCodeAscIdAsc(
                        preparation.getId()),
                dispatchRepository.findByPreparationIdAndDeletedAtIsNullOrderByCustomerNameAscSiteNameAscIdAsc(
                        preparation.getId()));
    }

    private void applyAssignment(
            DailyPreparationAssignmentSaveRequest request,
            DailyPreparationAssignment entity,
            DailyPreparation preparation,
            Employee employee) {
        Customer customer = request.getCustomerId() != null
                ? findCustomer(request.getCustomerId())
                : null;

        CustomerSite site = request.getCustomerSiteId() != null
                ? findSite(request.getCustomerSiteId())
                : null;

        if (site != null) {
            customer = resolveCustomerForSite(customer, site);
        }

        entity.setPreparationId(preparation.getId());

        entity.setEmployeeId(employee.getId());
        entity.setEmployeeCode(employee.getEmployeeCode());
        entity.setEmployeeName(employee.getEmployeeName());

        entity.setCustomerId(customer != null ? customer.getId() : null);
        entity.setCustomerName(customer != null ? customer.getName() : null);

        entity.setCustomerSiteId(site != null ? site.getId() : null);
        entity.setSiteName(site != null ? site.getName() : null);

        entity.setWorkDescription(request.getWorkDescription());
    }

    private void applyDispatch(
            DailyPreparationDispatchSaveRequest request,
            DailyPreparationDispatch entity,
            DailyPreparation preparation,
            CustomerSite site) {
        Customer customer = request.getCustomerId() != null
                ? findCustomer(request.getCustomerId())
                : null;

        customer = resolveCustomerForSite(customer, site);

        entity.setPreparationId(preparation.getId());

        entity.setCustomerId(customer != null ? customer.getId() : null);
        entity.setCustomerName(customer != null ? customer.getName() : null);

        entity.setCustomerSiteId(site.getId());
        entity.setSiteName(site.getName());
        entity.setDistanceFromCompanyKm(site.getDistanceFromCompanyKm());

        entity.setVehicleCount(request.getVehicleCount() != null ? request.getVehicleCount() : 0);
        entity.setNote(request.getNote());
    }

    private void validateAssignmentRequest(DailyPreparationAssignmentSaveRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("リクエストが不正です。");
        }

        if (request.getPreparationId() == null) {
            throw new IllegalArgumentException("preparationId は必須です。");
        }

        if (request.getEmployeeId() == null) {
            throw new IllegalArgumentException("employeeId は必須です。");
        }
    }

    private void validateDispatchRequest(DailyPreparationDispatchSaveRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("リクエストが不正です。");
        }

        if (request.getPreparationId() == null) {
            throw new IllegalArgumentException("preparationId は必須です。");
        }

        if (request.getCustomerSiteId() == null) {
            throw new IllegalArgumentException("customerSiteId は必須です。");
        }

        if (request.getVehicleCount() != null
                && request.getVehicleCount() < 0) {
            throw new IllegalArgumentException("配車台数は0以上で指定してください。");
        }
    }

    private DailyPreparation findPreparation(Long id) {
        return preparationRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new IllegalArgumentException("翌日準備が見つかりません。 id=" + id));
    }

    private Employee findEmployee(Long id) {
        return employeeRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new IllegalArgumentException("従業員が見つかりません。 id=" + id));
    }

    @SuppressWarnings("null")
    private Customer findCustomer(Long id) {
        return customerRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new IllegalArgumentException("顧客が見つかりません。 id=" + id));
    }

    @SuppressWarnings("null")
    private CustomerSite findSite(Long id) {
        return customerSiteRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new IllegalArgumentException("現場が見つかりません。 id=" + id));
    }

    private Customer resolveCustomerForSite(
            Customer requestedCustomer,
            CustomerSite site
    ) {
        Customer siteCustomer = findCustomer(site.getCustomerId());
        if (requestedCustomer != null
                && !Objects.equals(requestedCustomer.getId(), siteCustomer.getId())) {
            throw new IllegalArgumentException(
                    "指定された顧客と現場の組み合わせが一致しません。"
            );
        }
        return siteCustomer;
    }

    private void requireSamePreparation(Long actualId, Long requestedId) {
        if (!Objects.equals(actualId, requestedId)) {
            throw new IllegalArgumentException(
                    "操作対象が指定された翌日準備に属していません。"
            );
        }
    }

    private void deleteAssignment(Long id, Long preparationId) {
        DailyPreparationAssignment entity = assignmentRepository
                .findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "従業員配置が見つかりません。 id=" + id
                ));
        requireSamePreparation(entity.getPreparationId(), preparationId);
        entity.setDeletedAt(Instant.now(clock));
    }

    private void deleteDispatch(Long id, Long preparationId) {
        DailyPreparationDispatch entity = dispatchRepository
                .findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "現場配車が見つかりません。 id=" + id
                ));
        requireSamePreparation(entity.getPreparationId(), preparationId);
        entity.setDeletedAt(Instant.now(clock));
    }

    private void validateBulkFlags(
            boolean newFlag,
            boolean updatedFlag,
            boolean deletedFlag,
            Long id
    ) {
        int enabledCount = (newFlag ? 1 : 0)
                + (updatedFlag ? 1 : 0)
                + (deletedFlag ? 1 : 0);
        if (enabledCount > 1) {
            throw new IllegalArgumentException(
                    "一括保存の操作区分を複数指定できません。"
            );
        }
        if ((updatedFlag || deletedFlag) && id == null) {
            throw new IllegalArgumentException(
                    "更新・削除対象のIDは必須です。"
            );
        }
        if (newFlag && id != null) {
            throw new IllegalArgumentException(
                    "新規行にはIDを指定できません。"
            );
        }
    }

    private void removeUnusedDispatches(Long preparationId) {
        Set<Long> activeSiteIds = new HashSet<>();
        assignmentRepository
                .findByPreparationIdAndDeletedAtIsNullOrderByEmployeeCodeAscIdAsc(preparationId)
                .stream()
                .map(DailyPreparationAssignment::getCustomerSiteId)
                .filter(Objects::nonNull)
                .forEach(activeSiteIds::add);

        dispatchRepository
                .findByPreparationIdAndDeletedAtIsNullOrderByCustomerNameAscSiteNameAscIdAsc(
                        preparationId
                )
                .stream()
                .filter(dispatch -> !activeSiteIds.contains(
                        dispatch.getCustomerSiteId()
                ))
                .forEach(dispatch -> dispatch.setDeletedAt(Instant.now(clock)));
    }

    private <T> List<T> safeItems(List<T> items) {
        return items == null ? List.of() : items;
    }

    private String normalizeNote(String note) {
        if (note == null || note.isBlank()) {
            return null;
        }
        String normalized = note.trim();
        if (normalized.length() > 1000) {
            throw new IllegalArgumentException("備考は1000文字以内で入力してください。");
        }
        return normalized;
    }
}
