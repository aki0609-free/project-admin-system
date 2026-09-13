package com.project.backend.features.customer.exception;

/**
 * 入金済み、または過入金の取引を顧客締めから更新しようとした場合の競合。
 */
public class CustomerTransactionAlreadySettledException
        extends RuntimeException {

    public CustomerTransactionAlreadySettledException(
            Long customerId,
            String targetMonth
    ) {
        super(
                "入金済みの取引が存在するため顧客締めできません。"
                        + "customerId=" + customerId
                        + ", targetMonth=" + targetMonth
        );
    }
}
