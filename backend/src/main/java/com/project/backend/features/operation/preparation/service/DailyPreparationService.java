package com.project.backend.features.operation.preparation.service;

import java.time.Instant;
import java.time.LocalDate;

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

    @Transactional(readOnly = true)
    public DailyPreparationResponse findByTargetDate(LocalDate targetDate) {
        if (targetDate == null) {
            throw new RuntimeException("targetDate は必須です。");
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
            throw new RuntimeException("targetDate は必須です。");
        }

        if (preparationRepository.existsByTargetDateAndDeletedAtIsNull(request.getTargetDate())) {
            throw new RuntimeException("指定日の翌日準備は既に存在します。");
        }

        DailyPreparation entity = new DailyPreparation();
        entity.setTargetDate(request.getTargetDate());
        entity.setStatus(DailyPreparationStatus.OPEN);
        entity.setNote(request.getNote());

        return toResponse(preparationRepository.save(entity));
    }

    public DailyPreparationAssignmentResponse createAssignment(
            DailyPreparationAssignmentSaveRequest request) {
        validateAssignmentRequest(request);

        DailyPreparation preparation = findPreparation(request.getPreparationId());
        Employee employee = findEmployee(request.getEmployeeId());

        if (assignmentRepository.existsByPreparationIdAndEmployeeIdAndDeletedAtIsNull(
                preparation.getId(),
                employee.getId())) {
            throw new RuntimeException("この従業員の配置は既に存在します。");
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
                .orElseThrow(() -> new RuntimeException("従業員配置が見つかりません。 id=" + id));

        applyAssignment(request, entity, preparation, employee);

        return mapper.toAssignmentResponse(assignmentRepository.save(entity));
    }

    public void deleteAssignment(Long id) {
        DailyPreparationAssignment entity = assignmentRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("従業員配置が見つかりません。 id=" + id));

        entity.setDeletedAt(Instant.now());
    }

    public DailyPreparationDispatchResponse createDispatch(
            DailyPreparationDispatchSaveRequest request) {
        validateDispatchRequest(request);

        DailyPreparation preparation = findPreparation(request.getPreparationId());
        CustomerSite site = findSite(request.getCustomerSiteId());

        if (dispatchRepository.existsByPreparationIdAndCustomerSiteIdAndDeletedAtIsNull(
                preparation.getId(),
                site.getId())) {
            throw new RuntimeException("この現場の配車は既に存在します。");
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
                .orElseThrow(() -> new RuntimeException("現場配車が見つかりません。 id=" + id));

        applyDispatch(request, entity, preparation, site);

        return mapper.toDispatchResponse(dispatchRepository.save(entity));
    }

    public void deleteDispatch(Long id) {
        DailyPreparationDispatch entity = dispatchRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("現場配車が見つかりません。 id=" + id));

        entity.setDeletedAt(Instant.now());
    }

    public DailyPreparationResponse bulkSaveAssignments(
            DailyPreparationAssignmentBulkSaveRequest request) {
        if (request == null || request.getPreparationId() == null) {
            throw new RuntimeException("preparationId は必須です。");
        }

        DailyPreparation preparation = findPreparation(request.getPreparationId());

        for (DailyPreparationAssignmentBulkSaveItemRequest item : request.getItems()) {
            if (item.isDeleted()) {
                if (item.getId() != null) {
                    deleteAssignment(item.getId());
                }
                continue;
            }

            if (item.isNew()) {
                DailyPreparationAssignmentSaveRequest saveRequest = toAssignmentSaveRequest(preparation.getId(), item);

                createAssignment(saveRequest);
                continue;
            }

            if (item.isUpdated()) {
                if (item.getId() == null) {
                    continue;
                }

                DailyPreparationAssignmentSaveRequest saveRequest = toAssignmentSaveRequest(preparation.getId(), item);

                updateAssignment(item.getId(), saveRequest);
            }
        }

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
            throw new RuntimeException("preparationId は必須です。");
        }

        DailyPreparation preparation = findPreparation(request.getPreparationId());

        for (DailyPreparationDispatchBulkSaveItemRequest item : request.getItems()) {
            if (item.isDeleted()) {
                if (item.getId() != null) {
                    deleteDispatch(item.getId());
                }
                continue;
            }

            if (item.isNew()) {
                DailyPreparationDispatchSaveRequest saveRequest = toDispatchSaveRequest(preparation.getId(), item);

                createDispatch(saveRequest);
                continue;
            }

            if (item.isUpdated()) {
                if (item.getId() == null) {
                    continue;
                }

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

        if (customer == null && site != null && site.getCustomerId() != null) {
            customer = findCustomer(site.getCustomerId());
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

        if (customer == null && site.getCustomerId() != null) {
            customer = findCustomer(site.getCustomerId());
        }

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
            throw new RuntimeException("リクエストが不正です。");
        }

        if (request.getPreparationId() == null) {
            throw new RuntimeException("preparationId は必須です。");
        }

        if (request.getEmployeeId() == null) {
            throw new RuntimeException("employeeId は必須です。");
        }
    }

    private void validateDispatchRequest(DailyPreparationDispatchSaveRequest request) {
        if (request == null) {
            throw new RuntimeException("リクエストが不正です。");
        }

        if (request.getPreparationId() == null) {
            throw new RuntimeException("preparationId は必須です。");
        }

        if (request.getCustomerSiteId() == null) {
            throw new RuntimeException("customerSiteId は必須です。");
        }
    }

    private DailyPreparation findPreparation(Long id) {
        return preparationRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("翌日準備が見つかりません。 id=" + id));
    }

    private Employee findEmployee(Long id) {
        return employeeRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("従業員が見つかりません。 id=" + id));
    }

    @SuppressWarnings("null")
    private Customer findCustomer(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("顧客が見つかりません。 id=" + id));
    }

    @SuppressWarnings("null")
    private CustomerSite findSite(Long id) {
        return customerSiteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("現場が見つかりません。 id=" + id));
    }
}