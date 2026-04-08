package kz.iqadam.esyllabus.config;

import java.net.URI;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public record ApplicationSecurityProperties(
        List<String> allowedRoles,
        URI postLoginRedirectUri
) {
}
