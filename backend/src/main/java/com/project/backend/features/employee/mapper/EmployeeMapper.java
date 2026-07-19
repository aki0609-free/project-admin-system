package com.project.backend.features.employee.mapper;

import java.math.BigDecimal;
import java.util.List;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import com.project.backend.features.employee.dto.EmployeeContractResponse;
import com.project.backend.features.employee.dto.EmployeeContractSaveRequest;
import com.project.backend.features.employee.dto.EmployeeDetailResponse;
import com.project.backend.features.employee.dto.EmployeeListItemResponse;
import com.project.backend.features.employee.dto.EmployeePayrollProfileResponse;
import com.project.backend.features.employee.dto.EmployeePayrollProfileSaveRequest;
import com.project.backend.features.employee.dto.EmployeeSaveRequest;
import com.project.backend.features.employee.entity.Employee;
import com.project.backend.features.employee.entity.EmployeeContract;
import com.project.backend.features.employee.entity.EmployeePayrollProfile;
import com.project.backend.features.employee.enums.EmploymentStatus;
import com.project.backend.features.employee.enums.EmploymentType;
import com.project.backend.features.employee.enums.PaymentCycle;
import com.project.backend.features.employee.enums.SalaryType;
import com.project.backend.features.employee.enums.TaxCategory;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface EmployeeMapper {

        List<EmployeeListItemResponse> toListItemResponseList(List<Employee> employees);

        EmployeeListItemResponse toListItemResponse(Employee employee);

        @Mapping(target = "id", source = "employee.id")
        @Mapping(target = "employeeCode", source = "employee.employeeCode")
        @Mapping(target = "employeeName", source = "employee.employeeName")
        @Mapping(target = "employeeNameKana", source = "employee.employeeNameKana")
        @Mapping(target = "gender", source = "employee.gender")
        @Mapping(target = "birthDate", source = "employee.birthDate")
        @Mapping(target = "hireDate", source = "employee.hireDate")
        @Mapping(target = "resignDate", source = "employee.resignDate")
        @Mapping(target = "employmentType", source = "employee.employmentType")
        @Mapping(target = "employmentStatus", source = "employee.employmentStatus")
        @Mapping(target = "phone", source = "employee.phone")
        @Mapping(target = "email", source = "employee.email")
        @Mapping(target = "postalCode", source = "employee.postalCode")
        @Mapping(target = "address", source = "employee.address")
        @Mapping(target = "activeFlag", source = "employee.activeFlag")
        @Mapping(target = "payrollProfile", source = "payrollProfile")
        @Mapping(target = "contract", source = "contract")
        EmployeeDetailResponse toDetailResponse(
                        Employee employee,
                        EmployeePayrollProfile payrollProfile,
                        EmployeeContract contract);

        EmployeePayrollProfileResponse toPayrollProfileResponse(EmployeePayrollProfile profile);

        EmployeeContractResponse toContractResponse(EmployeeContract contract);

        void updateEmployeeFromRequest(
                        EmployeeSaveRequest request,
                        @MappingTarget Employee employee);

        void updatePayrollProfileFromRequest(
                        EmployeePayrollProfileSaveRequest request,
                        @MappingTarget EmployeePayrollProfile profile);

        void updateContractFromRequest(
                        EmployeeContractSaveRequest request,
                        @MappingTarget EmployeeContract contract);

        @AfterMapping
        default void afterUpdateEmployeeFromRequest(
                        EmployeeSaveRequest request,
                        @MappingTarget Employee employee) {
                employee.setEmploymentType(
                                request.employmentType() != null
                                                ? request.employmentType()
                                                : EmploymentType.FULL_TIME);

                employee.setEmploymentStatus(
                                request.employmentStatus() != null
                                                ? request.employmentStatus()
                                                : EmploymentStatus.ACTIVE);

                employee.setActiveFlag(
                                request.activeFlag() == null || request.activeFlag());
        }

        @AfterMapping
        default void afterUpdatePayrollProfileFromRequest(
                        EmployeePayrollProfileSaveRequest request,
                        @MappingTarget EmployeePayrollProfile profile) {
                if (request == null) {
                        profile.setTaxCategory(TaxCategory.KOU);
                        profile.setTaxDependentCount(0);
                        profile.setDependentFlag(false);
                        profile.setDependentOfOtherFlag(false);
                        profile.setPaidLeaveRemainingDays(BigDecimal.ZERO);
                        profile.setIncomeTaxCalcFlag(true);
                        profile.setResidentTaxCalcFlag(true);
                        profile.setResidentTaxMonthly(BigDecimal.ZERO);
                        profile.setEmploymentInsuranceFlag(true);
                        profile.setSocialInsuranceFlag(true);
                        profile.setHealthInsuranceFlag(true);
                        profile.setPensionInsuranceFlag(true);
                        profile.setCareInsuranceFlag(false);
                        profile.setDailyPayFlag(false);
                        profile.setCommuteAllowanceMonthly(BigDecimal.ZERO);
                        return;
                }

                profile.setTaxCategory(
                                request.taxCategory() != null
                                                ? request.taxCategory()
                                                : TaxCategory.KOU);

                profile.setTaxDependentCount(
                                request.taxDependentCount() != null
                                                ? request.taxDependentCount()
                                                : 0);

                profile.setDependentFlag(Boolean.TRUE.equals(request.dependentFlag()));
                profile.setDependentOfOtherFlag(Boolean.TRUE.equals(request.dependentOfOtherFlag()));
                profile.setPaidLeaveRemainingDays(nvl(request.paidLeaveRemainingDays()));

                profile.setIncomeTaxCalcFlag(
                                request.incomeTaxCalcFlag() == null || request.incomeTaxCalcFlag());

                profile.setResidentTaxCalcFlag(
                                request.residentTaxCalcFlag() == null || request.residentTaxCalcFlag());

                profile.setResidentTaxMonthly(nvl(request.residentTaxMonthly()));

                profile.setEmploymentInsuranceFlag(
                                request.employmentInsuranceFlag() == null || request.employmentInsuranceFlag());

                profile.setSocialInsuranceFlag(
                                request.socialInsuranceFlag() == null || request.socialInsuranceFlag());

                profile.setHealthInsuranceFlag(
                                request.healthInsuranceFlag() == null || request.healthInsuranceFlag());

                profile.setPensionInsuranceFlag(
                                request.pensionInsuranceFlag() == null || request.pensionInsuranceFlag());

                profile.setCareInsuranceFlag(Boolean.TRUE.equals(request.careInsuranceFlag()));
                profile.setDailyPayFlag(Boolean.TRUE.equals(request.dailyPayFlag()));
                profile.setCommuteAllowanceMonthly(nvl(request.commuteAllowanceMonthly()));
        }

        @AfterMapping
        default void afterUpdateContractFromRequest(
                        EmployeeContractSaveRequest request,
                        @MappingTarget EmployeeContract contract) {
                if (request == null) {
                        contract.setRenewalFlag(false);
                        contract.setSalaryType(SalaryType.MONTHLY);
                        contract.setPaymentCycle(PaymentCycle.MONTHLY);
                        contract.setMonthlySalary(BigDecimal.ZERO);
                        contract.setWeeklyWage(BigDecimal.ZERO);
                        contract.setDailyWage(BigDecimal.ZERO);
                        contract.setHourlyWage(BigDecimal.ZERO);
                        contract.setStandardWorkingHours(BigDecimal.ZERO);
                        return;
                }

                contract.setRenewalFlag(Boolean.TRUE.equals(request.renewalFlag()));

                contract.setSalaryType(
                                request.salaryType() != null
                                                ? request.salaryType()
                                                : SalaryType.MONTHLY);

                contract.setPaymentCycle(
                                request.paymentCycle() != null
                                                ? request.paymentCycle()
                                                : PaymentCycle.MONTHLY);

                contract.setMonthlySalary(nvl(request.monthlySalary()));
                contract.setWeeklyWage(nvl(request.weeklyWage()));
                contract.setDailyWage(nvl(request.dailyWage()));
                contract.setHourlyWage(nvl(request.hourlyWage()));
                contract.setStandardWorkingHours(nvl(request.standardWorkingHours()));
        }

        default BigDecimal nvl(BigDecimal value) {
                return value != null ? value : BigDecimal.ZERO;
        }
}