package com.project.backend.features.admin.document.enums;

public enum DocumentArea {
    GENERAL("会社書類"),
    GENERATED_REPORTS("生成帳票"),
    BACKUPS("バックアップ"),
    TEMPLATES("テンプレート"),
    IMPORT_SCRIPTS("取込スクリプト");

    private final String displayName;

    DocumentArea(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
