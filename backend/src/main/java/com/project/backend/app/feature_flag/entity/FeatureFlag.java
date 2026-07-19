package com.project.backend.app.feature_flag.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "feature_flag")
@Getter
@Setter
public class FeatureFlag {

    @Id
    @Column(name = "feature_key")
    private String key;

    private Boolean enabled;
    
}
