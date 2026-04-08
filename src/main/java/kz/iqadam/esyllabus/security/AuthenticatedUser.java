package kz.iqadam.esyllabus.security;

import java.util.Set;

public record AuthenticatedUser(
        String email,
        String displayName,
        Set<String> roles
) {
}
