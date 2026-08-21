package com.project.backend.features.master.payrollitem.parameter;

public interface PayrollItemRuleParameterValueResolver {

    String key();

    Object resolve(PayrollItemRuleParameterResolutionContext context);
}
