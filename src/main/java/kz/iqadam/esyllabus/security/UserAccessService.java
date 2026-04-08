package kz.iqadam.esyllabus.security;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import kz.iqadam.esyllabus.config.ApplicationSecurityProperties;
import kz.iqadam.esyllabus.integration.digital.DigitalUniversityRoleClient;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class UserAccessService {

    private final Set<String> allowedRoles;
    private final DigitalUniversityRoleClient digitalUniversityRoleClient;

    public UserAccessService(
            ApplicationSecurityProperties properties,
            DigitalUniversityRoleClient digitalUniversityRoleClient
    ) {
        this.allowedRoles = properties.allowedRoles().stream()
                .map(UserAccessService::normalizeRole)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        this.digitalUniversityRoleClient = digitalUniversityRoleClient;
    }

    public AuthenticatedUser authorize(Map<String, Object> attributes) {
        var email = extractEmail(attributes);
        var displayName = Objects.toString(attributes.getOrDefault("name", email), email);
        var roles = digitalUniversityRoleClient.getRolesByEmail(email).stream()
                .map(UserAccessService::normalizeRole)
                .filter(role -> !role.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (roles.stream().noneMatch(allowedRoles::contains)) {
            throw new AccessDeniedException("User is not allowed to access ESyllabus");
        }

        return new AuthenticatedUser(email, displayName, roles);
    }

    private String extractEmail(Map<String, Object> attributes) {
        return Set.of("preferred_username", "email", "upn").stream()
                .map(attributes::get)
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .filter(value -> !value.isBlank())
                .findFirst()
                .orElseThrow(() -> new AccessDeniedException("Email was not provided by Microsoft"));
    }

    public static String normalizeRole(String role) {
        return role == null
                ? ""
                : role.trim()
                        .replace('-', '_')
                        .replace(' ', '_')
                        .toUpperCase(Locale.ROOT);
    }
}
