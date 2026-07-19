package com.project.backend.features.system.batch.controller;

import org.springframework.web.bind.annotation.*;

import com.project.backend.features.system.batch.service.BatchDynamicSchedulerService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/system/batch/schedules")
@RequiredArgsConstructor
public class BatchScheduleController {

    private final BatchDynamicSchedulerService schedulerService;

    @PostMapping("/reload")
    public void reloadAll() {
        schedulerService.reloadSchedules();
    }

    @PostMapping("/{id}/reload")
    public void reloadOne(@PathVariable Long id) {
        schedulerService.reloadJob(id);
    }

    @PostMapping("/{id}/cancel")
    public void cancel(@PathVariable Long id) {
        schedulerService.cancel(id);
    }
}