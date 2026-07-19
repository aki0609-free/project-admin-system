package com.project.backend.app.feature_flag.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.app.feature_flag.entity.FeatureFlag;

public interface FeatureFlagRepository extends JpaRepository<FeatureFlag, String> {
    
}
