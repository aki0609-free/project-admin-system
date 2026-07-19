package com.project.backend.features.system.imports.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.project.backend.features.system.imports.properties.ImportScriptProperties;

@Configuration
@EnableConfigurationProperties(ImportScriptProperties.class)
public class ImportScriptConfig {
}