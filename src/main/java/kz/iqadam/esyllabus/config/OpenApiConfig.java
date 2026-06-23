package kz.iqadam.esyllabus.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BASIC_AUTH_SCHEME = "basicAuth";
    private static final String DIGITAL_UNIVERSITY_BEARER_SCHEME = "digitalUniversityBearer";

    @Bean
    OpenAPI esyllabusOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("ESyllabus API")
                        .version("v1")
                        .description("API for managing courses, syllabi, and library resources"))
                .components(new Components()
                        .addSecuritySchemes(BASIC_AUTH_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("basic"))
                        .addSecuritySchemes(DIGITAL_UNIVERSITY_BEARER_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BASIC_AUTH_SCHEME))
                .addSecurityItem(new SecurityRequirement().addList(DIGITAL_UNIVERSITY_BEARER_SCHEME));
    }
}
