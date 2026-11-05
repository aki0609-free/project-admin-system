package com.project.backend.features.system.masterdata.contract;

import java.util.Map;

public record GenericMasterRecordCandidate(
        Long id,
        Map<String, Object> values,
        boolean activeFlag
) {
}
