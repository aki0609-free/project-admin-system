package com.project.backend.app.storage.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.project.backend.app.storage.enums.StorageType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "project.storage")
public class StorageProperties {

    /**
     * 旧設定 project.storage.type との互換用。
     */
    private StorageType type;

    /**
     * 新規保存時に使用するデフォルトストレージ。
     */
    private StorageType defaultType;

    private String localBasePath = "storage";

    private Template template = new Template();

    private Output output = new Output();

    private Import imports = new Import();

    private S3 s3 = new S3();

    /**
     * 既存コード向けの互換アクセサー。
     */
    public StorageType getType() {
        return resolveDefaultType();
    }

    public StorageType resolveDefaultType() {
        if (defaultType != null) {
            return defaultType;
        }

        if (type != null) {
            return type;
        }

        return StorageType.LOCAL;
    }

    @Getter
    @Setter
    public static class Template {
        private String path = "templates/reports";
    }

    @Getter
    @Setter
    public static class Output {
        private String path = "reports";
    }

    @Getter
    @Setter
    public static class Import {
        private Script script = new Script();
        private Csv csv = new Csv();
    }

    @Getter
    @Setter
    public static class Script {
        private String path = "imports/scripts";
    }

    @Getter
    @Setter
    public static class Csv {
        private String path = "imports/csv";
    }

    @Getter
    @Setter
    public static class S3 {
        private boolean enabled;
        private String bucket;
        private String region = "ap-northeast-1";
    }
}
