package com.project.backend.features.system.mail.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.project.backend.features.system.mail.entity.MailRecipientGroup;

public interface MailRecipientGroupRepository extends JpaRepository<MailRecipientGroup, Long> {
    /** 論理削除Filterの影響を受けず、外部同期用の一意キーから取得する。 */
    @Query(
            value = "SELECT * FROM mail_recipient_group "
                    + "WHERE group_key = :groupKey LIMIT 1",
            nativeQuery = true
    )
    Optional<MailRecipientGroup> findByGroupKeyIncludingDeleted(
            @Param("groupKey") String groupKey
    );

    Optional<MailRecipientGroup> findByGroupKeyAndActiveFlagTrueAndDeletedAtIsNull(String groupKey);

    List<MailRecipientGroup> findAllByDeletedAtIsNullOrderByIdAsc();

    Optional<MailRecipientGroup> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByGroupKeyAndDeletedAtIsNull(String groupKey);

    boolean existsByGroupKeyAndIdNotAndDeletedAtIsNull(String groupKey, Long id);
}
