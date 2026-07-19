package com.project.backend.features.system.mail.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.project.backend.features.system.mail.dto.MailSendQueueResponse;
import com.project.backend.features.system.mail.entity.MailSendQueue;

@Mapper(uses = MailAddressMapper.class)
public interface MailSendQueueMapper {

    @Mapping(target = "toAddresses", source = "toAddress", qualifiedByName = "splitAddresses")
    @Mapping(target = "ccAddresses", source = "cc", qualifiedByName = "splitAddresses")
    @Mapping(target = "bccAddresses", source = "bcc", qualifiedByName = "splitAddresses")
    MailSendQueueResponse toResponse(MailSendQueue entity);

    List<MailSendQueueResponse> toResponseList(List<MailSendQueue> entities);
}