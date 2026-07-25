package com.project.backend.features.system.rule.service;

import java.util.List;
import java.util.Map;

import org.jeasy.rules.api.Rule;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityNotFoundException;

@Service
public class RuleBeanCatalogService {

    private final Map<String, Rule> ruleBeans;

    public RuleBeanCatalogService(Map<String, Rule> ruleBeans) {
        this.ruleBeans = Map.copyOf(ruleBeans);
    }

    public List<String> findBeanNames() {
        return ruleBeans.keySet().stream()
                .sorted()
                .toList();
    }

    public void requireRegistered(String beanName) {
        if (!ruleBeans.containsKey(beanName)) {
            throw new EntityNotFoundException(
                    "登録済みRule Beanが見つかりません。 beanName="
                            + beanName
            );
        }
    }
}
