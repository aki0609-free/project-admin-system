package com.project.backend.app.feature_flag.services;

import org.springframework.stereotype.Service;

import com.project.backend.app.feature_flag.entity.FeatureFlag;
import com.project.backend.app.feature_flag.repository.FeatureFlagRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FeatureFlagService {
    
    private final FeatureFlagRepository repository;

    @SuppressWarnings("null")
    public boolean isEnabled(String key) {

        return repository
                .findById(key)
                .map(FeatureFlag::getEnabled)
                .orElse(false);
    }
}
