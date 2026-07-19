package com.project.backend.features.system.imports.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "project.imports.script")
public class ImportScriptProperties {

    /**
     * Pythonスクリプト実行時のpythonコマンド。
     */
    private String pythonCommand;
}