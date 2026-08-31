package com.project.backend.features.customer.service.integration;

/**
 * 顧客担当者と請求書送付先グループの同期境界。
 *
 * <p>顧客ドメインがメール管理のEntityやRepositoryへ直接依存しないためのPort。</p>
 */
public interface CustomerInvoiceMailGroupSynchronizer {

    void synchronize(Long customerId, String customerName);

    void delete(Long customerId);
}
