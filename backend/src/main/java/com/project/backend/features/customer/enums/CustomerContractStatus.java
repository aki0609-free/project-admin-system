package com.project.backend.features.customer.enums;

/**
 * 顧客との契約状態。
 *
 * <p>ACTIVE の顧客だけを業務上の選択候補・顧客締め対象として扱う。</p>
 */
public enum CustomerContractStatus {
    ACTIVE,
    INACTIVE,
    ENDED
}
