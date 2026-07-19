package com.project.backend.features.system.mail.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.project.backend.features.system.mail.entity.MailRecipientGroup;

public interface MailRecipientGroupRepository extends JpaRepository<MailRecipientGroup, Long> {
    Optional<MailRecipientGroup> findByGroupKeyAndActiveFlagTrueAndDeletedAtIsNull(String groupKey);

    List<MailRecipientGroup> findAllByDeletedAtIsNullOrderByIdAsc();

    Optional<MailRecipientGroup> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByGroupKeyAndDeletedAtIsNull(String groupKey);

    boolean existsByGroupKeyAndIdNotAndDeletedAtIsNull(String groupKey, Long id);
}