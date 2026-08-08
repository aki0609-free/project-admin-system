package com.project.backend.features.employee.entity;

import com.project.backend.app.base.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "employee_resignation_setting",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_employee_resignation_setting_code",
                columnNames = {"tenant_id", "setting_code"}
        )
)
@Getter
@Setter
public class EmployeeResignationSetting extends BaseEntity {

    public static final String DEFAULT_SETTING_CODE = "DEFAULT";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "setting_code", nullable = false, length = 50)
    private String settingCode = DEFAULT_SETTING_CODE;

    @Column(name = "dialog_title", nullable = false, length = 200)
    private String dialogTitle = "退職処理";

    @Column(name = "guidance_message", nullable = false, length = 2000)
    private String guidanceMessage =
            "退職日と確認項目を確認してから退職処理を実行してください。";

    @Column(name = "confirmation_message", nullable = false, length = 500)
    private String confirmationMessage =
            "実行すると従業員の在籍状態が退職になります。";
}
