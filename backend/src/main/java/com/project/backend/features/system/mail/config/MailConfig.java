package com.project.backend.features.system.mail.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.project.backend.features.system.mail.properties.MailProperties;

@Configuration
@EnableConfigurationProperties(MailProperties.class)
public class MailConfig {
}