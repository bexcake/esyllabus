package kz.iqadam.esyllabus.security;

import java.util.Map;
import java.util.Set;

public record DigitalUniversityJwtClaims(
        String principal,
        String displayName,
        Set<String> roles,
        Map<String, Object> claims
) {
}
