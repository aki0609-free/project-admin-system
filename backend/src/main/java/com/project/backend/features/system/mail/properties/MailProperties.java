package com.project.backend.features.system.mail.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "project.mail")
public class MailProperties {

    private String fromAddress;
    private String fromName;
}