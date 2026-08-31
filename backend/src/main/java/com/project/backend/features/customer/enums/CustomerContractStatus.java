package com.project.backend.features.customer.enums;

/**
 * 顧客との契約状態。
 *
 * <p>現時点では顧客情報の管理・表示に使用し、締め処理などの業務可否は制御しない。</p>
 */
public enum CustomerContractStatus {
    ACTIVE,
    INACTIVE,
    ENDED
}
