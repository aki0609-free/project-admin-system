package com.project.backend.features.system.mail.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.project.backend.features.system.mail.dto.MailRecipientGroupResponse;
import com.project.backend.features.system.mail.dto.MailRecipientGroupSaveRequest;
import com.project.backend.features.system.mail.service.MailRecipientGroupAdminService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/system/mail-recipient-groups")
@RequiredArgsConstructor
public class MailRecipientGroupAdminController {

    private final MailRecipientGroupAdminService service;

    @GetMapping
    public List<MailRecipientGroupResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public MailRecipientGroupResponse findDetail(@PathVariable Long id) {
        return service.findDetail(id);
    }

    @PostMapping
    public MailRecipientGroupResponse create(
            @RequestBody MailRecipientGroupSaveRequest request
    ) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public MailRecipientGroupResponse update(
            @PathVariable Long id,
            @RequestBody MailRecipientGroupSaveRequest request
    ) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}