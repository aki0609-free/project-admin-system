package com.project.backend.features.system.mail.service.loader;

import com.project.backend.features.system.mail.dto.MailAttachmentResource;
import com.project.backend.features.system.mail.entity.MailSendAttachment;

public interface MailAttachmentLoader {

    boolean exists(MailSendAttachment attachment);

    MailAttachmentResource load(MailSendAttachment attachment);
}
