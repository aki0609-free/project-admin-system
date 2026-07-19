package com.project.backend.features.system.mail.controller;

import org.springframework.web.bind.annotation.*;

import com.project.backend.features.system.mail.dto.MailPdfSendRequest;
import com.project.backend.features.system.mail.dto.MailSendResult;
import com.project.backend.features.system.mail.service.MailPdfSendService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/system/mail/pdf")
@RequiredArgsConstructor
public class MailPdfSendController {

    private final MailPdfSendService service;

    @PostMapping("/send")
    public MailSendResult sendPdf(@RequestBody MailPdfSendRequest request) {
        return service.send(request);
    }
}