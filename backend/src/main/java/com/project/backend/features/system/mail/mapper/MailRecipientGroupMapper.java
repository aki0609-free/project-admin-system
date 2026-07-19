package com.project.backend.features.system.mail.mapper;

import java.util.List;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import com.project.backend.features.system.mail.dto.MailRecipientGroupResponse;
import com.project.backend.features.system.mail.dto.MailRecipientGroupSaveRequest;
import com.project.backend.features.system.mail.dto.MailRecipientResponse;
import com.project.backend.features.system.mail.dto.MailRecipientSaveRequest;
import com.project.backend.features.system.mail.entity.MailRecipient;
import com.project.backend.features.system.mail.entity.MailRecipientGroup;

@Mapper
public interface MailRecipientGroupMapper {

    @Mapping(target = "recipients", source = "recipients", qualifiedByName = "toActiveRecipientResponses")
    MailRecipientGroupResponse toResponse(MailRecipientGroup entity);

    List<MailRecipientGroupResponse> toResponseList(List<MailRecipientGroup> entities);

    MailRecipientResponse toRecipientResponse(MailRecipient entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "group", ignore = true)
    @Mapping(target = "recipientType", expression = "java(request.recipientTypeOrDefault())")
    MailRecipient toRecipientEntity(MailRecipientSaveRequest request);

    @Named("toActiveRecipientResponses")
    default List<MailRecipientResponse> toActiveRecipientResponses(List<MailRecipient> recipients) {
        if (recipients == null) {
            return List.of();
        }

        return recipients.stream()
                .filter(item -> item.getDeletedAt() == null)
                .map(this::toRecipientResponse)
                .toList();
    }

    default List<MailRecipient> toRecipientEntities(MailRecipientGroupSaveRequest request) {
        return request.recipientsOrEmpty().stream()
                .map(this::toRecipientEntity)
                .toList();
    }

    @AfterMapping
    default void clearBackReference(@MappingTarget MailRecipient recipient) {
        recipient.setGroup(null);
    }
}