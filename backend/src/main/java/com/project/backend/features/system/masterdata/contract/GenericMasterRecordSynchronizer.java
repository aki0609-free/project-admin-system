package com.project.backend.features.system.masterdata.contract;

import java.util.List;

/**
 * 汎用マスターを正本としつつ、既存業務テーブルを互換参照用に同期する契約。
 */
public interface GenericMasterRecordSynchronizer {

    String masterCode();

    void synchronize(List<GenericMasterRecordCandidate> records);
}
