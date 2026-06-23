package kz.iqadam.esyllabus.security;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

public record DigitalUniversityJwtClaims(
        String principal,
        String displayName,
        Long userId,
        Instant expiresAt,
        Set<String> roles,
        Map<String, Object> claims
) {
}
