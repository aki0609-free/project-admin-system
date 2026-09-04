package com.project.backend.features.employee.mapper;

import java.math.BigDecimal;

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
import com.project.backend.features.employee.enums.PaymentCycle;
import com.project.backend.features.employee.enums.SalaryType;
import com.project.backend.features.employee.enums.TaxCategory;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface EmployeeMapper {

        @Mapping(target = "contractStartDate", source = "contract.contractStartDate")
        @Mapping(target = "contractEndDate", source = "contract.contractEndDate")
        @Mapping(target = "id", source = "employee.id")
        EmployeeListItemResponse toListItemResponse(
                        Employee employee,
                        EmployeeContract contract);

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
        @Mapping(target = "payrollItemSettings", source = "payrollItemSettings")
        EmployeeDetailResponse toDetailResponse(
                        Employee employee,
                        EmployeePayrollProfile payrollProfile,
                        EmployeeContract contract,
                        java.util.List<com.project.backend.features.employee.dto.EmployeePayrollItemSettingResponse> payrollItemSettings);

        EmployeePayrollProfileResponse toPayrollProfileResponse(EmployeePayrollProfile profile);

        EmployeeContractResponse toContractResponse(EmployeeContract contract);

        @Mapping(target = "employeeCode", ignore = true)
        @Mapping(target = "resignDate", ignore = true)
        @Mapping(target = "employmentStatus", ignore = true)
        @Mapping(target = "activeFlag", ignore = true)
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
