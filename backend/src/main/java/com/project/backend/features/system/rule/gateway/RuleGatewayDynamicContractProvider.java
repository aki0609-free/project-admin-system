package com.project.backend.features.system.rule.gateway;

public interface RuleGatewayDynamicContractProvider {

    boolean supports(String gatewayKey);

    RuleGatewayContract contract(String gatewayKey);
}
