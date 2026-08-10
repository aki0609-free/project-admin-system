package com.project.backend.testsupport;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.testcontainers.containers.Container;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.MountableFile;

public final class RuntimeSchemaAssetInstaller {

    private static final String MANIFEST = "sql/runtime-schema-manifest.txt";
    private static final String CONTAINER_DIRECTORY =
            "/tmp/project-admin-runtime-schema";

    private RuntimeSchemaAssetInstaller() {
    }

    public static List<String> readManifest() throws IOException {
        ClassLoader classLoader = RuntimeSchemaAssetInstaller.class
                .getClassLoader();
        try (var input = classLoader.getResourceAsStream(MANIFEST)) {
            if (input == null) {
                throw new IllegalStateException(
                        "SQL資産マニフェストが見つかりません: " + MANIFEST
                );
            }
            try (var reader = new BufferedReader(new InputStreamReader(
                    input,
                    StandardCharsets.UTF_8
            ))) {
                return reader.lines()
                        .map(String::trim)
                        .filter(line -> !line.isEmpty())
                        .filter(line -> !line.startsWith("#"))
                        .toList();
            }
        }
    }

    public static void apply(
            MySQLContainer<?> mysql,
            List<String> resources
    ) throws IOException, InterruptedException {
        AtomicInteger sequence = new AtomicInteger();
        for (String resource : resources) {
            String target = CONTAINER_DIRECTORY + "/"
                    + String.format("%02d-", sequence.incrementAndGet())
                    + resource.substring(resource.lastIndexOf('/') + 1);
            mysql.copyFileToContainer(
                    MountableFile.forClasspathResource(resource),
                    target
            );

            Container.ExecResult result = mysql.execInContainer(
                    "sh",
                    "-c",
                    "mysql --protocol=tcp --host=127.0.0.1 "
                            + "--default-character-set=utf8mb4 "
                            + "--user=\"$MYSQL_USER\" "
                            + "--password=\"$MYSQL_PASSWORD\" "
                            + "\"$MYSQL_DATABASE\" < \"" + target + "\""
            );

            assertThat(result.getExitCode())
                    .as("SQL資産の適用: %s\nstdout:%n%s\nstderr:%n%s",
                            resource,
                            result.getStdout(),
                            result.getStderr())
                    .isZero();
        }
    }
}
