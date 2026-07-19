package com.project.backend.features.system.mail.mapper;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import com.project.backend.features.system.mail.dto.MailQueueCreateRequest;
import com.project.backend.features.system.mail.entity.MailSendAttachment;
import com.project.backend.features.system.mail.entity.MailSendQueue;

@Mapper(
        componentModel = "spring",
        uses = MailAddressMapper.class,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface MailQueueMapper {

    @Mapping(target = "toAddress", source = "toAddresses", qualifiedByName = "joinAddresses")
    @Mapping(target = "cc", source = "ccAddresses", qualifiedByName = "joinAddresses")
    @Mapping(target = "bcc", source = "bccAddresses", qualifiedByName = "joinAddresses")
    MailSendQueue toEntity(MailQueueCreateRequest request);

    @AfterMapping
    default void linkAttachments(@MappingTarget MailSendQueue queue) {
        if (queue.getAttachments() == null) {
            return;
        }

        for (MailSendAttachment attachment : queue.getAttachments()) {
            attachment.setMailSendQueue(queue);
        }
    }
}