package com.project.backend.features.system.mail.repository;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import com.project.backend.features.system.mail.entity.MailSendQueue;
import com.project.backend.features.system.mail.enums.MailSendStatus;

public interface MailSendQueueRepository extends JpaRepository<MailSendQueue, Long> {

    List<MailSendQueue> findTop50ByStatusAndDeletedAtIsNullOrderByIdAsc(MailSendStatus status);

    List<MailSendQueue> findAllByDeletedAtIsNullOrderByIdDesc();

    Optional<MailSendQueue> findByIdAndDeletedAtIsNull(Long id);

    List<MailSendQueue> findAllByIdInAndDeletedAtIsNullOrderByIdAsc(List<Long> ids);

    List<MailSendQueue> findByBusinessKeyAndDeletedAtIsNull(String businessKey);

    boolean existsByBusinessKeyAndStatusAndDeletedAtIsNull(
            String businessKey,
            MailSendStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<MailSendQueue> findAllByBusinessKeyAndDeletedAtIsNullOrderByIdAsc(
            String businessKey
    );

    List<MailSendQueue> findByMailTypeAndStatusAndDeletedAtIsNullOrderByIdAsc(
            String mailType,
            MailSendStatus status
    );
}
