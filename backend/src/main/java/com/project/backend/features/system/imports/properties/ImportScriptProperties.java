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

    /**
     * スクリプトの最大実行秒数。
     */
    private long timeoutSeconds = 120;

    /**
     * エラー表示へ保持する標準出力の最大文字数。
     */
    private int maxOutputCharacters = 20000;
}
