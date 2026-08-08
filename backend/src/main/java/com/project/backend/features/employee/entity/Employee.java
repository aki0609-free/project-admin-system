package com.project.backend.features.employee.entity;

import java.time.LocalDate;

import com.project.backend.app.base.entity.BaseEntity;
import com.project.backend.features.employee.enums.EmploymentStatus;
import com.project.backend.features.employee.enums.EmploymentType;
import com.project.backend.features.employee.enums.Gender;
import com.project.backend.features.employee.enums.DormitoryType;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "employee",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"tenant_id", "employee_code"})
        }
)
@Getter
@Setter
public class Employee extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_code", nullable = false, length = 100)
    private String employeeCode;

    @Column(name = "employee_name", nullable = false, length = 200)
    private String employeeName;

    @Column(name = "employee_name_kana", length = 200)
    private String employeeNameKana;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 30)
    private Gender gender;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "hire_date")
    private LocalDate hireDate;

    @Column(name = "resign_date")
    private LocalDate resignDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", nullable = false, length = 30)
    private EmploymentType employmentType = EmploymentType.FULL_TIME;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_status", nullable = false, length = 30)
    private EmploymentStatus employmentStatus = EmploymentStatus.ACTIVE;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "dormitory_flag", nullable = false)
    private boolean dormitoryFlag = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "dormitory_type", length = 30)
    private DormitoryType dormitoryType;

    @Column(name = "active_flag", nullable = false)
    private boolean activeFlag = true;

    public void initializeEmployment() {
        this.employmentStatus = EmploymentStatus.ACTIVE;
        this.resignDate = null;
        this.activeFlag = true;
    }

    public void updateDormitory(boolean resident, DormitoryType type) {
        if (resident && type == null) {
            throw new IllegalArgumentException("入寮ありの場合は寮タイプを選択してください。");
        }
        this.dormitoryFlag = resident;
        this.dormitoryType = resident ? type : null;
    }

    public void changeEmploymentStatus(EmploymentStatus status) {
        if (status == null || status == EmploymentStatus.RESIGNED) {
            throw new IllegalArgumentException(
                    "退職状態への変更は退職処理から実行してください。"
            );
        }
        this.employmentStatus = status;
        this.resignDate = null;
        this.activeFlag = true;
    }

    public void resign(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("退職日は必須です。");
        }
        if (hireDate != null && date.isBefore(hireDate)) {
            throw new IllegalArgumentException(
                    "退職日は入社日以降で指定してください。"
            );
        }
        this.resignDate = date;
        this.employmentStatus = EmploymentStatus.RESIGNED;
        this.activeFlag = false;
    }

    public void cancelResignation(EmploymentStatus restoredStatus) {
        if (this.employmentStatus != EmploymentStatus.RESIGNED) {
            throw new IllegalStateException("退職済みの従業員ではありません。");
        }
        changeEmploymentStatus(restoredStatus);
    }
}
