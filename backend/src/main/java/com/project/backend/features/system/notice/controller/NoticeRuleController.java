package com.project.backend.features.system.notice.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.project.backend.features.system.notice.dto.NoticeRuleResponse;
import com.project.backend.features.system.notice.dto.NoticeRuleSaveRequest;
import com.project.backend.features.system.notice.service.NoticeRuleCommandService;
import com.project.backend.features.system.notice.service.NoticeRuleQueryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/system/notice-rules")
@RequiredArgsConstructor
public class NoticeRuleController {

    private final NoticeRuleQueryService queryService;
    private final NoticeRuleCommandService commandService;

    @GetMapping
    public List<NoticeRuleResponse> findAll() {
        return queryService.findAll();
    }

    @GetMapping("/{id}")
    public NoticeRuleResponse findDetail(
            @PathVariable Long id
    ) {
        return queryService.findDetail(id);
    }

    @PostMapping
    public NoticeRuleResponse create(
            @RequestBody NoticeRuleSaveRequest request
    ) {
        return commandService.create(request);
    }

    @PutMapping("/{id}")
    public NoticeRuleResponse update(
            @PathVariable Long id,
            @RequestBody NoticeRuleSaveRequest request
    ) {
        return commandService.update(
                id,
                request
        );
    }

    @DeleteMapping("/{id}")
    public void delete(
            @PathVariable Long id
    ) {
        commandService.delete(id);
    }
}