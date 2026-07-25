package com.project.backend.features.system.notice.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import com.project.backend.features.system.notice.dto.NoticeGenerateResult;
import com.project.backend.features.system.notice.service.NoticeAutoGenerateService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/system/notice-rules/generate")
@PreAuthorize("hasRole('SYS_ADMIN')")
@RequiredArgsConstructor
public class NoticeRuleGenerateController {

    private final NoticeAutoGenerateService service;

    @PostMapping
    public NoticeGenerateResult generateAll() {
        return service.generateAll();
    }

    @PostMapping("/{ruleId}")
    public NoticeGenerateResult generateByRuleId(@PathVariable Long ruleId) {
        return service.generateByRuleId(ruleId);
    }
}
