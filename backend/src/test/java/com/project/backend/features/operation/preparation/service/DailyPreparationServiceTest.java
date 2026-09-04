package com.project.backend.features.operation.preparation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.project.backend.features.customer.entity.Customer;
import com.project.backend.features.customer.entity.CustomerSite;
import com.project.backend.features.customer.repository.CustomerRepository;
import com.project.backend.features.customer.repository.CustomerSiteRepository;
import com.project.backend.features.employee.entity.Employee;
import com.project.backend.features.employee.repository.EmployeeRepository;
import com.project.backend.features.operation.preparation.dto.DailyPreparationAssignmentBulkSaveItemRequest;
import com.project.backend.features.operation.preparation.dto.DailyPreparationAssignmentBulkSaveRequest;
import com.project.backend.features.operation.preparation.dto.DailyPreparationAssignmentSaveRequest;
import com.project.backend.features.operation.preparation.dto.DailyPreparationDispatchSaveRequest;
import com.project.backend.features.operation.preparation.entity.DailyPreparation;
import com.project.backend.features.operation.preparation.entity.DailyPreparationAssignment;
import com.project.backend.features.operation.preparation.entity.DailyPreparationDispatch;
import com.project.backend.features.operation.preparation.mapper.DailyPreparationMapper;
import com.project.backend.features.operation.preparation.repository.DailyPreparationAssignmentRepository;
import com.project.backend.features.operation.preparation.repository.DailyPreparationDispatchRepository;
import com.project.backend.features.operation.preparation.repository.DailyPreparationRepository;

class DailyPreparationServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-05T00:00:00Z");

    private DailyPreparationRepository preparationRepository;
    private DailyPreparationAssignmentRepository assignmentRepository;
    private DailyPreparationDispatchRepository dispatchRepository;
    private EmployeeRepository employeeRepository;
    private CustomerRepository customerRepository;
    private CustomerSiteRepository siteRepository;
    private DailyPreparationService service;

    @BeforeEach
    void setUp() {
        preparationRepository = mock(DailyPreparationRepository.class);
        assignmentRepository = mock(DailyPreparationAssignmentRepository.class);
        dispatchRepository = mock(DailyPreparationDispatchRepository.class);
        employeeRepository = mock(EmployeeRepository.class);
        customerRepository = mock(CustomerRepository.class);
        siteRepository = mock(CustomerSiteRepository.class);
        service = new DailyPreparationService(
                preparationRepository,
                assignmentRepository,
                dispatchRepository,
                employeeRepository,
                customerRepository,
                siteRepository,
                new DailyPreparationMapper(),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void createAssignment_shouldRejectCustomerAndSiteOwnedByDifferentCustomer() {
        DailyPreparation preparation = preparation(1L);
        Employee employee = employee(10L);
        Customer customerA = customer(20L, "顧客A");
        Customer customerB = customer(21L, "顧客B");
        CustomerSite site = site(30L, customerB.getId(), "B現場");
        DailyPreparationAssignmentSaveRequest request = assignmentRequest(
                preparation.getId(),
                employee.getId(),
                customerA.getId(),
                site.getId()
        );

        when(preparationRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(preparation));
        when(employeeRepository.findByIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(employee));
        when(customerRepository.findByIdAndDeletedAtIsNull(20L))
                .thenReturn(Optional.of(customerA));
        when(customerRepository.findByIdAndDeletedAtIsNull(21L))
                .thenReturn(Optional.of(customerB));
        when(siteRepository.findByIdAndDeletedAtIsNull(30L))
                .thenReturn(Optional.of(site));

        assertThatThrownBy(() -> service.createAssignment(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("組み合わせが一致しません");
    }

    @Test
    void createDispatch_shouldRejectNegativeVehicleCount() {
        DailyPreparationDispatchSaveRequest request = new DailyPreparationDispatchSaveRequest();
        request.setPreparationId(1L);
        request.setCustomerSiteId(30L);
        request.setVehicleCount(-1);

        assertThatThrownBy(() -> service.createDispatch(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("0以上");
    }

    @Test
    void bulkSaveAssignments_shouldRejectUpdatedRowWithoutId() {
        DailyPreparation preparation = preparation(1L);
        DailyPreparationAssignmentBulkSaveItemRequest item =
                new DailyPreparationAssignmentBulkSaveItemRequest();
        item.setEmployeeId(10L);
        item.setUpdatedFlag(true);
        DailyPreparationAssignmentBulkSaveRequest request =
                new DailyPreparationAssignmentBulkSaveRequest();
        request.setPreparationId(1L);
        request.setItems(List.of(item));
        when(preparationRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(preparation));

        assertThatThrownBy(() -> service.bulkSaveAssignments(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("IDは必須");
    }

    @Test
    void bulkSaveAssignments_shouldSoftDeleteDispatchWithoutActiveAssignment() {
        DailyPreparation preparation = preparation(1L);
        DailyPreparationAssignment assignment = new DailyPreparationAssignment();
        assignment.setId(11L);
        assignment.setPreparationId(1L);
        assignment.setEmployeeId(10L);
        DailyPreparationDispatch dispatch = new DailyPreparationDispatch();
        dispatch.setId(40L);
        dispatch.setPreparationId(1L);
        dispatch.setCustomerSiteId(30L);

        DailyPreparationAssignmentBulkSaveItemRequest item =
                new DailyPreparationAssignmentBulkSaveItemRequest();
        item.setId(assignment.getId());
        item.setEmployeeId(assignment.getEmployeeId());
        item.setDeletedFlag(true);
        DailyPreparationAssignmentBulkSaveRequest request =
                new DailyPreparationAssignmentBulkSaveRequest();
        request.setPreparationId(1L);
        request.setItems(List.of(item));

        when(preparationRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(preparation));
        when(assignmentRepository.findByIdAndDeletedAtIsNull(11L))
                .thenReturn(Optional.of(assignment));
        when(assignmentRepository
                .findByPreparationIdAndDeletedAtIsNullOrderByEmployeeCodeAscIdAsc(1L))
                .thenReturn(List.of());
        when(dispatchRepository
                .findByPreparationIdAndDeletedAtIsNullOrderByCustomerNameAscSiteNameAscIdAsc(1L))
                .thenReturn(List.of(dispatch));

        service.bulkSaveAssignments(request);

        assertThat(assignment.getDeletedAt()).isEqualTo(NOW);
        assertThat(dispatch.getDeletedAt()).isEqualTo(NOW);
    }

    @Test
    void updateNote_shouldKeepTrimmedMemo() {
        DailyPreparation preparation = preparation(1L);
        when(preparationRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(preparation));
        when(preparationRepository.save(preparation)).thenReturn(preparation);
        when(assignmentRepository
                .findByPreparationIdAndDeletedAtIsNullOrderByEmployeeCodeAscIdAsc(1L))
                .thenReturn(List.of());
        when(dispatchRepository
                .findByPreparationIdAndDeletedAtIsNullOrderByCustomerNameAscSiteNameAscIdAsc(1L))
                .thenReturn(List.of());

        service.updateNote(1L, "  集合場所は正面玄関  ");

        assertThat(preparation.getNote()).isEqualTo("集合場所は正面玄関");
        verify(preparationRepository).save(preparation);
    }

    private DailyPreparation preparation(Long id) {
        DailyPreparation value = new DailyPreparation();
        value.setId(id);
        return value;
    }

    private Employee employee(Long id) {
        Employee value = new Employee();
        value.setId(id);
        value.setEmployeeCode("E001");
        value.setEmployeeName("テスト従業員");
        return value;
    }

    private Customer customer(Long id, String name) {
        Customer value = new Customer();
        value.setId(id);
        value.setName(name);
        return value;
    }

    private CustomerSite site(Long id, Long customerId, String name) {
        CustomerSite value = new CustomerSite();
        value.setId(id);
        value.setCustomerId(customerId);
        value.setName(name);
        return value;
    }

    private DailyPreparationAssignmentSaveRequest assignmentRequest(
            Long preparationId,
            Long employeeId,
            Long customerId,
            Long siteId
    ) {
        DailyPreparationAssignmentSaveRequest request =
                new DailyPreparationAssignmentSaveRequest();
        request.setPreparationId(preparationId);
        request.setEmployeeId(employeeId);
        request.setCustomerId(customerId);
        request.setCustomerSiteId(siteId);
        return request;
    }
}
