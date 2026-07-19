package com.project.backend.features.system.report.property;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "keystore")
@Getter
@Setter
public class KeyStoreProperties {
    private String path;
    private String password;
    private String alias;
}
