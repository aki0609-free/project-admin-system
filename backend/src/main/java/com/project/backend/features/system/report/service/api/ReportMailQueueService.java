package com.project.backend.features.system.report.service.api;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.system.mail.entity.MailSendQueue;
import com.project.backend.features.system.mail.service.MailQueueCreateService;
import com.project.backend.features.system.report.entity.ReportOutputFile;
import com.project.backend.features.system.report.enums.ReportOutputFileStatus;
import com.project.backend.features.system.report.repository.ReportOutputFileRepository;
import com.project.backend.features.system.report.service.builder.ReportMailQueueCreateRequestBuilder;
import com.project.backend.features.system.report.service.support.ReportErrorMessageLimiter;
import com.project.backend.features.system.report.service.validation.ReportMailQueueValidator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportMailQueueService {

    private final ReportOutputFileRepository reportOutputFileRepository;
    private final MailQueueCreateService mailQueueCreateService;
    private final ReportMailQueueValidator validator;
    private final ReportMailQueueCreateRequestBuilder requestBuilder;
    private final ReportErrorMessageLimiter errorMessageLimiter;

    @Transactional
    public int createMailQueuesByMailType(String mailType) {
        List<ReportOutputFile> files =
                reportOutputFileRepository.findByMailTypeAndStatusAndDeletedAtIsNullOrderByIdAsc(
                        mailType,
                        ReportOutputFileStatus.CREATED
                );

        int count = 0;

        for (ReportOutputFile file : files) {
            if (createMailQueue(file) != null) {
                count++;
            }
        }

        return count;
    }

    @Transactional
    public List<Long> createMailQueuesByExecutionId(String executionId) {
        List<ReportOutputFile> files =
                reportOutputFileRepository
                        .findByExecutionIdAndDeletedAtIsNullOrderByIdAsc(executionId)
                        .stream()
                        .filter(file -> file.getStatus() == ReportOutputFileStatus.CREATED)
                        .toList();

        List<Long> queueIds = new ArrayList<>();

        for (ReportOutputFile file : files) {
            Long queueId = createMailQueue(file);

            if (queueId != null) {
                queueIds.add(queueId);
            }
        }

        return List.copyOf(queueIds);
    }

    private Long createMailQueue(ReportOutputFile file) {
        try {
            validator.validate(file);

            MailSendQueue queue =
                    mailQueueCreateService.createDraftFromTemplate(
                            requestBuilder.build(file)
                    );

            file.setStatus(ReportOutputFileStatus.MAIL_QUEUED);
            file.setErrorMessage(null);

            return queue != null ? queue.getId() : null;

        } catch (Exception e) {
            file.setStatus(ReportOutputFileStatus.FAILED);
            file.setErrorMessage(errorMessageLimiter.limit(e.getMessage()));
            return null;
        }
    }

}
