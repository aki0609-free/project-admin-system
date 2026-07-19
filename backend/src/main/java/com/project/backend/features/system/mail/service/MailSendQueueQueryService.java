package com.project.backend.features.system.mail.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.system.mail.dto.MailSendQueueResponse;
import com.project.backend.features.system.mail.mapper.MailSendQueueMapper;
import com.project.backend.features.system.mail.repository.MailSendQueueRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MailSendQueueQueryService {

    private final MailSendQueueRepository repository;
    private final MailSendQueueMapper mapper;

    @Transactional(readOnly = true)
    public List<MailSendQueueResponse> findAll() {
        return mapper.toResponseList(
                repository.findAllByDeletedAtIsNullOrderByIdDesc()
        );
    }
}