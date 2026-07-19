package com.project.backend.features.system.rule.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.project.backend.features.system.rule.dto.RuleMasterResponse;
import com.project.backend.features.system.rule.dto.RuleMasterSaveRequest;
import com.project.backend.features.system.rule.service.RuleMasterCommandService;
import com.project.backend.features.system.rule.service.RuleMasterQueryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/system/rules")
@RequiredArgsConstructor
public class RuleMasterController {

    private final RuleMasterQueryService queryService;
    private final RuleMasterCommandService commandService;

    @GetMapping
    public List<RuleMasterResponse> findAll() {
        return queryService.findAll();
    }

    @GetMapping("/active")
    public List<RuleMasterResponse> findActiveRules() {
        return queryService.findActiveRules();
    }

    @GetMapping("/{id}")
    public RuleMasterResponse findDetail(@PathVariable Long id) {
        return queryService.findDetail(id);
    }

    @GetMapping("/by-name/{ruleName}")
    public RuleMasterResponse findByRuleName(@PathVariable String ruleName) {
        return queryService.findByRuleName(ruleName);
    }

    @PostMapping
    public RuleMasterResponse create(@RequestBody RuleMasterSaveRequest request) {
        return commandService.create(request);
    }

    @PutMapping("/{id}")
    public RuleMasterResponse update(
            @PathVariable Long id,
            @RequestBody RuleMasterSaveRequest request
    ) {
        return commandService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        commandService.delete(id);
    }
}