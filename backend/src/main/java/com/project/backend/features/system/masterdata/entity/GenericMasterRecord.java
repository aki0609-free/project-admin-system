package com.project.backend.features.system.masterdata.entity;

import com.project.backend.app.base.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "generic_master_record")
@Getter
@Setter
public class GenericMasterRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "definition_id", nullable = false)
    private Long definitionId;

    @Column(name = "values_json", nullable = false, columnDefinition = "JSON")
    private String valuesJson;

    @Column(name = "active_flag", nullable = false)
    private boolean activeFlag;

    @Version
    @Column(name = "lock_version", nullable = false)
    private long lockVersion;
}
