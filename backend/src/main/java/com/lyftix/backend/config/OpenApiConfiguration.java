package com.lyftix.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI lyftixOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Lyftix API")
                        .description("Personal analytics platform API")
                        .version("1.0.0"));
    }
}
