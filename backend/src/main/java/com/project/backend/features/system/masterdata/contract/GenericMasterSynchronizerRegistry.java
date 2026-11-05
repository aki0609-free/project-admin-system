package com.project.backend.features.system.masterdata.contract;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

@Component
public class GenericMasterSynchronizerRegistry {

    private final Map<String, GenericMasterRecordSynchronizer> synchronizers;

    public GenericMasterSynchronizerRegistry(
            List<GenericMasterRecordSynchronizer> synchronizers
    ) {
        this.synchronizers = synchronizers.stream().collect(Collectors.toUnmodifiableMap(
                GenericMasterRecordSynchronizer::masterCode,
                Function.identity()
        ));
    }

    public void synchronize(
            String masterCode,
            List<GenericMasterRecordCandidate> records
    ) {
        GenericMasterRecordSynchronizer synchronizer = synchronizers.get(masterCode);
        if (synchronizer != null) {
            synchronizer.synchronize(records);
        }
    }
}
