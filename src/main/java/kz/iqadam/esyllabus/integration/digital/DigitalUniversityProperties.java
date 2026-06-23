package kz.iqadam.esyllabus.integration.digital;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "digital-university")
public record DigitalUniversityProperties(
        boolean enabled,
        URI baseUrl,
        String studentPath,
        String studentsPath,
        String employeesPath,
        String employeePath,
        String schoolsPath,
        String educationProgramsPath,
        String teacherDisciplinesPath,
        Duration connectTimeout,
        Duration readTimeout,
        Jwt jwt
) {

    public record Jwt(
            boolean enabled,
            String secret,
            Duration clockSkew,
            List<String> principalClaims,
            List<String> displayNameClaims,
            List<String> roleClaims
    ) {
    }
}
