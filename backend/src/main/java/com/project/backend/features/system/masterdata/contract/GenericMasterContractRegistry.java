package com.project.backend.features.system.masterdata.contract;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

@Component
public class GenericMasterContractRegistry {

    private final Map<String, GenericMasterBusinessContract> contracts;

    public GenericMasterContractRegistry(List<GenericMasterBusinessContract> contracts) {
        this.contracts = contracts.stream().collect(Collectors.toUnmodifiableMap(
                GenericMasterBusinessContract::masterCode,
                Function.identity()
        ));
    }

    public void validate(
            String masterCode,
            List<GenericMasterRecordCandidate> records
    ) {
        GenericMasterBusinessContract contract = contracts.get(masterCode);
        if (contract != null) {
            contract.validate(records);
        }
    }
}
