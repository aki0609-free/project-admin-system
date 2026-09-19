package com.project.backend.features.dailyreport.enums;

/**
 * 日報1件に対応する車両の手配区分。
 *
 * <p>会社手配と社員手配は顧客への距離請求、社員手配は加えて
 * 従業員への運転手当の判定に使用する。同乗者は請求・運転手当の対象外とする。
 * 現場の選択だけからは推測しない。</p>
 */
public enum VehicleArrangementType {
    NONE,
    COMPANY,
    EMPLOYEE,
    PASSENGER
}
