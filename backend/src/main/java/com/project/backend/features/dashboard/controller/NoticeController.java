package com.project.backend.features.dashboard.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.project.backend.features.dashboard.dto.NoticeResponse;
import com.project.backend.features.dashboard.dto.NoticeSaveRequest;
import com.project.backend.features.dashboard.service.NoticeCommandService;
import com.project.backend.features.dashboard.service.NoticeQueryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class NoticeController {

    private final NoticeQueryService queryService;
    private final NoticeCommandService commandService;

    @GetMapping
    public List<NoticeResponse> findAll() {
        return queryService.findAll();
    }

    @GetMapping("/calendar")
    public List<NoticeResponse> findByPeriod(
            @RequestParam LocalDate from,
            @RequestParam LocalDate to
    ) {
        return queryService.findByPeriod(
                from,
                to
        );
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SYS_ADMIN', 'ADMIN')")
    public NoticeResponse create(
            @RequestBody NoticeSaveRequest request
    ) {
        return commandService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYS_ADMIN', 'ADMIN')")
    public NoticeResponse update(
            @PathVariable Long id,
            @RequestBody NoticeSaveRequest request
    ) {
        return commandService.update(
                id,
                request
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('SYS_ADMIN', 'ADMIN')")
    public void delete(
            @PathVariable Long id
    ) {
        commandService.delete(id);
    }
}
