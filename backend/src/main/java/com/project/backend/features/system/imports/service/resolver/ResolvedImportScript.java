package com.project.backend.features.system.imports.service.resolver;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

public record ResolvedImportScript(
        Path path,
        Path cleanupRoot
) implements AutoCloseable {

    public static ResolvedImportScript local(Path path) {
        return new ResolvedImportScript(path, null);
    }

    public static ResolvedImportScript temporary(
            Path path,
            Path cleanupRoot
    ) {
        return new ResolvedImportScript(path, cleanupRoot);
    }

    @Override
    public void close() {
        if (cleanupRoot == null || !Files.exists(cleanupRoot)) {
            return;
        }

        try (var paths = Files.walk(cleanupRoot)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(current -> {
                        try {
                            Files.deleteIfExists(current);
                        } catch (Exception ignored) {
                            // 一時ファイル削除失敗で取込結果を失敗にしない。
                        }
                    });
        } catch (Exception ignored) {
            // 一時ディレクトリが存在しない場合を含めて無視する。
        }
    }
}
