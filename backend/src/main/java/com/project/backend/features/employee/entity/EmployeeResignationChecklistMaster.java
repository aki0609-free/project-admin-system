package com.project.backend.features.employee.entity;

import com.project.backend.app.base.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "employee_resignation_checklist_master")
@Getter
@Setter
public class EmployeeResignationChecklistMaster extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 100)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "required_flag", nullable = false)
    private boolean requiredFlag = true;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    @Column(name = "active_flag", nullable = false)
    private boolean activeFlag = true;
}