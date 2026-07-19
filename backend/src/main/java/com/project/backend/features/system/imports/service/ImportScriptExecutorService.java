package com.project.backend.features.system.imports.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.project.backend.features.system.imports.dto.ImportTargetDefinition;
import com.project.backend.features.system.imports.enums.ImportScriptType;
import com.project.backend.features.system.imports.properties.ImportScriptProperties;
import com.project.backend.features.system.imports.service.resolver.ImportScriptPathResolver;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ImportScriptExecutorService {

    private final ImportScriptProperties properties;
    private final ImportScriptPathResolver scriptPathResolver;

    public void execute(ImportTargetDefinition target) {
        validate(target);

        try {
            Path resolvedScriptPath = scriptPathResolver.resolve(target.scriptPath());

            List<String> command = buildCommand(
                    target,
                    resolvedScriptPath
            );

            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(true);

            Process process = builder.start();

            String output = readOutput(process);
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new RuntimeException(
                        "CSV生成スクリプトが失敗しました。 exitCode="
                                + exitCode
                                + System.lineSeparator()
                                + output
                );
            }

        } catch (Exception e) {
            throw new RuntimeException("CSV生成スクリプト実行に失敗しました。", e);
        }
    }

    private void validate(ImportTargetDefinition target) {
        if (target.scriptType() == null || target.scriptType() == ImportScriptType.NONE) {
            throw new RuntimeException("scriptType が設定されていません。 targetCode=" + target.targetCode());
        }

        if (!StringUtils.hasText(target.scriptPath())) {
            throw new RuntimeException("scriptPath が設定されていません。 targetCode=" + target.targetCode());
        }

        if (target.scriptType() == ImportScriptType.PYTHON
                && !StringUtils.hasText(properties.getPythonCommand())) {
            throw new RuntimeException("Python実行コマンドが設定されていません。 project.imports.script.python-command");
        }
    }

    private List<String> buildCommand(
            ImportTargetDefinition target,
            Path resolvedScriptPath
    ) {
        List<String> command = new ArrayList<>();

        if (target.scriptType() == ImportScriptType.PYTHON) {
            command.add(properties.getPythonCommand());
        } else if (target.scriptType() == ImportScriptType.SHELL) {
            command.add("sh");
        } else {
            throw new RuntimeException("未対応のscriptTypeです。 scriptType=" + target.scriptType());
        }

        command.add(resolvedScriptPath.toString());

        if (StringUtils.hasText(target.scriptArgs())) {
            command.addAll(splitArgs(target.scriptArgs()));
        }

        return command;
    }

    private String readOutput(Process process) throws Exception {
        StringBuilder output = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
        )) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
        }

        return output.toString();
    }

    private List<String> splitArgs(String args) {
        List<String> result = new ArrayList<>();

        for (String arg : args.trim().split("\\s+")) {
            if (!arg.isBlank()) {
                result.add(arg);
            }
        }

        return result;
    }
}