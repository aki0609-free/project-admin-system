package com.project.backend.features.employee.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import com.project.backend.features.employee.dto.EmployeeListItemResponse;
import com.project.backend.features.employee.entity.Employee;
import com.project.backend.features.employee.entity.EmployeeContract;
import com.project.backend.features.employee.enums.EmploymentStatus;
import com.project.backend.features.employee.enums.EmploymentType;

class EmployeeMapperTest {

    private final EmployeeMapper mapper = Mappers.getMapper(EmployeeMapper.class);

    @Test
    void toListItemResponse_shouldExposeRegisteredAddressAndBirthDate() {
        Employee employee = new Employee();
        employee.setId(1L);
        employee.setEmployeeCode("E001");
        employee.setEmployeeName("一覧確認 太郎");
        employee.setAddress("東京都千代田区テスト1-2-3");
        employee.setBirthDate(LocalDate.of(1990, 11, 13));
        employee.setEmploymentType(EmploymentType.FULL_TIME);
        employee.setEmploymentStatus(EmploymentStatus.ACTIVE);

        EmployeeContract contract = new EmployeeContract();
        contract.setContractStartDate(LocalDate.of(2026, 4, 1));

        EmployeeListItemResponse response = mapper.toListItemResponse(employee, contract);

        assertThat(response.address()).isEqualTo("東京都千代田区テスト1-2-3");
        assertThat(response.birthDate()).isEqualTo(LocalDate.of(1990, 11, 13));
    }
}
