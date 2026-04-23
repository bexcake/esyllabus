package kz.iqadam.esyllabus.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public record ApplicationSecurityProperties(
        List<LocalUserProperties> users
) {
    public record LocalUserProperties(
            String username,
            String password,
            List<String> roles
    ) {
    }
}
