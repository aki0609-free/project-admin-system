package com.project.backend.features.master.payrollitem.transaction;

/**
 * 明細取引の業務上の役割。
 */
public enum PayrollItemTransactionPurpose {
    /** 請求・権利の発生。残高だけを増やし、月次給与額へ直接加算しない。 */
    BALANCE_ACCRUAL,
    /** 日報を経由せず、月次給与へ直接反映する手当・控除。 */
    PAYROLL_ITEM
}
