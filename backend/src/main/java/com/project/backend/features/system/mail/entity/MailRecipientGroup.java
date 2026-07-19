package com.project.backend.features.system.mail.entity;

import java.util.ArrayList;
import java.util.List;

import com.project.backend.app.base.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "mail_recipient_group")
@Getter
@Setter
public class MailRecipientGroup extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_key", nullable = false, unique = true, length = 100)
    private String groupKey;

    @Column(name = "group_name", nullable = false, length = 200)
    private String groupName;

    @Column(name = "active_flag", nullable = false)
    private boolean activeFlag = true;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<MailRecipient> recipients = new ArrayList<>();
}