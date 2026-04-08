package kz.iqadam.esyllabus.integration.digital;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "digital-university")
public record DigitalUniversityProperties(
        boolean enabled,
        URI baseUrl,
        String rolesPath,
        Duration connectTimeout,
        Duration readTimeout,
        String authHeaderName,
        String authHeaderValue
) {
}
