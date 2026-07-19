package com.project.backend.app.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

    public OpenAPI apiInfo() {

        return new OpenAPI()
                .info(new Info()
                        .title("Project API")
                        .description("Backend API")
                        .version("1.0") 
                );
    }
    
}
