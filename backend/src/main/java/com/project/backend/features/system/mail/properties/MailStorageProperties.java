package com.project.backend.features.system.mail.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "project.mail.storage")
public class MailStorageProperties {

    private String s3Bucket;
}