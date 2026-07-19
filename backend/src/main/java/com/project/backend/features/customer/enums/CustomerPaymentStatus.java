package com.project.backend.features.customer.enums;

public enum CustomerPaymentStatus {
    UNPAID,     // 未入金
    PARTIAL,    // 一部入金
    PAID,       // 入金済
    OVERPAID,   // 過入金
    CANCELED,   // 請求取消
    WRITE_OFF   // 貸倒
}