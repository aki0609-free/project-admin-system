package com.project.backend.features.system.mail.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.project.backend.features.system.mail.dto.MailActivateDraftRequest;
import com.project.backend.features.system.mail.dto.MailSendQueueResponse;
import com.project.backend.features.system.mail.dto.MailSendResult;
import com.project.backend.features.system.mail.dto.MailTestSendRequest;
import com.project.backend.features.system.mail.service.MailSendQueueQueryService;
import com.project.backend.features.system.mail.service.MailSendService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/system/mail")
@RequiredArgsConstructor
public class MailSendController {

    private final MailSendService mailSendService;
    private final MailSendQueueQueryService queryService;

    @GetMapping("/queues")
    public List<MailSendQueueResponse> findQueues() {
        return queryService.findAll();
    }

    @PostMapping("/activate-draft")
    public MailSendResult activateDraft(@RequestBody MailActivateDraftRequest request) {
        if (request.hasBusinessKey()) {
            return mailSendService.activateDraftByBusinessKey(request.businessKey());
        }

        if (request.hasMailType()) {
            return mailSendService.activateDraftByMailType(request.mailType());
        }

        throw new RuntimeException("mailType または businessKey を指定してください。");
    }

    @PostMapping("/send-waiting")
    public MailSendResult sendWaiting() {
        return mailSendService.sendWaitingMails();
    }

    @PostMapping("/{id}/retry")
    public void retry(@PathVariable Long id) {
        mailSendService.retryFailed(id);
    }

    @PostMapping("/test-send")
    public MailSendResult testSend(@RequestBody MailTestSendRequest request) {
        return mailSendService.sendTestMail(request);
    }
}