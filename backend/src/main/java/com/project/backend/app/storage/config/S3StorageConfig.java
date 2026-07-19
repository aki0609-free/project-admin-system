package com.project.backend.app.storage.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.project.backend.app.storage.properties.StorageProperties;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class S3StorageConfig {

    @Bean
    @ConditionalOnProperty(
            prefix = "project.storage.s3",
            name = "enabled",
            havingValue = "true"
    )
    public S3Client s3Client(StorageProperties properties) {
        return S3Client.builder()
                .region(Region.of(properties.getS3().getRegion()))
                .build();
    }
}
