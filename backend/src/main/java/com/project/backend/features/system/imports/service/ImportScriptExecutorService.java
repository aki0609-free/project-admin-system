package com.project.backend.features.system.imports.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.project.backend.features.system.imports.dto.ImportTargetDefinition;
import com.project.backend.features.system.imports.enums.ImportScriptType;
import com.project.backend.features.system.imports.properties.ImportScriptProperties;
import com.project.backend.features.system.imports.service.resolver.ImportCsvPathResolver;
import com.project.backend.features.system.imports.service.resolver.ImportScriptPathResolver;
import com.project.backend.features.system.imports.service.resolver.ResolvedImportScript;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ImportScriptExecutorService {

    private final ImportScriptProperties properties;
    private final ImportScriptPathResolver scriptPathResolver;
    private final ImportCsvPathResolver csvPathResolver;

    public void execute(ImportTargetDefinition target) {
        execute(target, null);
    }

    public void execute(ImportTargetDefinition target, Path inputFile) {
        validate(target);

        try (ResolvedImportScript resolvedScript =
                     scriptPathResolver.resolve(target.scriptPath())) {

            List<String> command = buildCommand(
                    target,
                    resolvedScript.path(),
                    inputFile
            );

            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(true);

            Process process = builder.start();

            CompletableFuture<String> outputFuture =
                    CompletableFuture.supplyAsync(
                            () -> readOutput(process)
                    );

            boolean finished = process.waitFor(
                    properties.getTimeoutSeconds(),
                    TimeUnit.SECONDS
            );

            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException(
                        "CSV生成スクリプトがタイムアウトしました。 timeoutSeconds="
                                + properties.getTimeoutSeconds()
                );
            }

            String output = outputFuture.get(5, TimeUnit.SECONDS);
            int exitCode = process.exitValue();

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
            Path resolvedScriptPath,
            Path inputFile
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
            command.addAll(splitArgs(target.scriptArgs(), inputFile));
        }

        return command;
    }

    private String readOutput(Process process) {
        StringBuilder output = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
        )) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (output.length()
                        < properties.getMaxOutputCharacters()) {
                    output.append(line)
                            .append(System.lineSeparator());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(
                    "スクリプトの標準出力を取得できませんでした。",
                    e
            );
        }

        if (output.length() > properties.getMaxOutputCharacters()) {
            return output.substring(
                    0,
                    properties.getMaxOutputCharacters()
            );
        }

        return output.toString();
    }

    private List<String> splitArgs(String args, Path inputFile) {
        List<String> result = new ArrayList<>();
        Path workDirectory = Path.of(properties.getWorkDirectory())
                .toAbsolutePath()
                .normalize();

        try {
            Files.createDirectories(workDirectory);
        } catch (Exception e) {
            throw new RuntimeException("取込作業ディレクトリを作成できません。", e);
        }

        for (String arg : args.trim().split("\\s+")) {
            if (!arg.isBlank()) {
                String resolved = arg.replace(
                        "${IMPORT_CSV_DIR}",
                        csvPathResolver.baseDirectory().toString()
                );
                resolved = resolved.replace(
                        "${IMPORT_WORK_DIR}",
                        workDirectory.toString()
                );
                resolved = resolved.replace(
                        "/tmp/project-admin",
                        workDirectory.toString()
                );
                if (resolved.contains("${IMPORT_INPUT_FILE}")) {
                    if (inputFile == null) {
                        throw new RuntimeException(
                                "IMPORT_INPUT_FILE が必要ですが入力ファイルがありません。"
                        );
                    }
                    resolved = resolved.replace(
                            "${IMPORT_INPUT_FILE}",
                            inputFile.toAbsolutePath().normalize().toString()
                    );
                }
                result.add(resolved);
            }
        }

        return result;
    }
}
