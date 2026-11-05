package com.project.backend.features.system.masterdata.contract;

import java.util.List;

/**
 * 汎用マスターの保存形式を保ったまま、業務固有の整合性検証だけを差し込む契約。
 */
public interface GenericMasterBusinessContract {

    String masterCode();

    void validate(List<GenericMasterRecordCandidate> records);
}
