package com.tenure.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tenure.storage.s3")
public record S3StorageProperties(
        String bucket,
        String region,
        String publicBaseUrl
) {
}
