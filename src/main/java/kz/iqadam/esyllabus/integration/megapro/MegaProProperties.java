package kz.iqadam.esyllabus.integration.megapro;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "megapro")
public record MegaProProperties(
        boolean enabled,
        URI baseUrl,
        String searchPath,
        Duration connectTimeout,
        Duration readTimeout,
        String authHeaderName,
        String authHeaderValue
) {
}
