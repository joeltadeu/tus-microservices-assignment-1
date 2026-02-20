package com.jts.pmanagement.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI testModuleOpenAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Patient Management Service API")
                .version("1.0.0")
                .description(
                    """
                       Healthcare Management API - Manage doctors, patients, and appointments. Supports full CRUD operations for doctors and patients, plus appointment scheduling, retrieval by patient, and cancellation.
                    """)
                .contact(
                    new Contact()
                        .name("Joel Silva")
                        .url("https://github.com/joeltadeu")
                        .email("joeltadeu@gmail.com"))
                .license(new License().name("MIT License").url("https://mit-license.org/")))
        .servers(
            List.of(
                new Server().url("http://localhost:9081").description("Local Development Server")));
  }
}
