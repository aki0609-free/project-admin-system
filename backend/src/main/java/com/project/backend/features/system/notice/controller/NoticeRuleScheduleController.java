package com.project.backend.features.system.notice.controller;

import org.springframework.web.bind.annotation.*;

import com.project.backend.features.system.notice.service.NoticeDynamicSchedulerService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/system/notice-rules/schedules")
@RequiredArgsConstructor
public class NoticeRuleScheduleController {

    private final NoticeDynamicSchedulerService schedulerService;

    @PostMapping("/reload")
    public void reload() {
        schedulerService.reloadSchedules();
    }

    @PostMapping("/{ruleId}/reload")
    public void reloadRule(@PathVariable Long ruleId) {
        schedulerService.reloadRule(ruleId);
    }

    @PostMapping("/{ruleId}/cancel")
    public void cancelRule(@PathVariable Long ruleId) {
        schedulerService.cancel(ruleId);
    }
}