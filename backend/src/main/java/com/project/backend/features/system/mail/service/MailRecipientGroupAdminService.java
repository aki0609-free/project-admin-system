package com.project.backend.features.system.mail.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.system.mail.dto.MailRecipientGroupResponse;
import com.project.backend.features.system.mail.dto.MailRecipientGroupSaveRequest;
import com.project.backend.features.system.mail.entity.MailRecipient;
import com.project.backend.features.system.mail.entity.MailRecipientGroup;
import com.project.backend.features.system.mail.mapper.MailRecipientGroupMapper;
import com.project.backend.features.system.mail.repository.MailRecipientGroupRepository;
import com.project.backend.features.system.mail.service.finder.MailRecipientGroupFinder;
import com.project.backend.features.system.mail.service.validation.MailRecipientGroupValidator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MailRecipientGroupAdminService {

    private final MailRecipientGroupRepository repository;
    private final MailRecipientGroupMapper mapper;
    private final MailRecipientGroupValidator validator;
    private final MailRecipientGroupFinder finder;

    @Transactional(readOnly = true)
    public List<MailRecipientGroupResponse> findAll() {
        return mapper.toResponseList(repository.findAllByDeletedAtIsNullOrderByIdAsc());
    }

    @Transactional(readOnly = true)
    public MailRecipientGroupResponse findDetail(Long id) {
        return mapper.toResponse(finder.getById(id));
    }

    @Transactional
    public MailRecipientGroupResponse create(MailRecipientGroupSaveRequest request) {
        validator.validateSaveRequest(request, null);

        MailRecipientGroup entity = new MailRecipientGroup();
        applyRequest(entity, request);

        return mapper.toResponse(repository.save(entity));
    }

    @SuppressWarnings("null")
    @Transactional
    public MailRecipientGroupResponse update(Long id, MailRecipientGroupSaveRequest request) {
        validator.validateSaveRequest(request, id);

        MailRecipientGroup entity = finder.getById(id);
        applyRequest(entity, request);

        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        MailRecipientGroup entity = finder.getById(id);

        Instant now = Instant.now();
        entity.setDeletedAt(now);

        if (entity.getRecipients() != null) {
            entity.getRecipients().forEach(recipient -> recipient.setDeletedAt(now));
        }
    }

    private void applyRequest(MailRecipientGroup entity, MailRecipientGroupSaveRequest request) {
        entity.setGroupKey(request.groupKey());
        entity.setGroupName(request.groupName());
        entity.setActiveFlag(request.activeFlag());

        replaceRecipients(entity, mapper.toRecipientEntities(request));
    }

    private void replaceRecipients(MailRecipientGroup entity, List<MailRecipient> recipients) {
        entity.getRecipients().clear();

        for (MailRecipient recipient : recipients) {
            recipient.setGroup(entity);
            entity.getRecipients().add(recipient);
        }
    }
}