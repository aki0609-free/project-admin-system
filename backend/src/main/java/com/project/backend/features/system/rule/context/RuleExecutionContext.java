package com.project.backend.features.system.rule.context;

import java.util.Map;

import com.project.backend.features.system.rule.entity.RuleMaster;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RuleExecutionContext {

    private RuleMaster rule;

    private Map<String,Object> parameters;

    private Map<String,Object> facts;

}