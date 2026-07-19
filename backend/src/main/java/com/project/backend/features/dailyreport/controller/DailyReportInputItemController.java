package com.project.backend.features.dailyreport.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.backend.features.dailyreport.dto.DailyReportInputResponse;
import com.project.backend.features.dailyreport.service.DailyReportInputItemService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/daily-reports/input-items")
public class DailyReportInputItemController {

    private final DailyReportInputItemService service;

    @GetMapping
    public DailyReportInputResponse findItems() {
        return service.findItems();
    }
}