package com.project.backend.features.system.mail.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.project.backend.features.system.mail.entity.MailTemplate;

public interface MailTemplateRepository extends JpaRepository<MailTemplate, Long> {

    List<MailTemplate> findAllByDeletedAtIsNullOrderByIdAsc();

    Optional<MailTemplate> findByIdAndDeletedAtIsNull(Long id);

    Optional<MailTemplate> findByTemplateKeyAndActiveFlagTrueAndDeletedAtIsNull(String templateKey);

    boolean existsByTemplateKey(String templateKey);
}
