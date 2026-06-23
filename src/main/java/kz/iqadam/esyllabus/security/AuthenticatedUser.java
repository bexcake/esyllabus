package kz.iqadam.esyllabus.security;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.Set;

public record AuthenticatedUser(
        String email,
        String displayName,
        Set<String> roles,
        Long employeeId,
        Long userId,
        String username,
        String schoolId,
        String schoolName,
        String positionTitle,
        String status,
        Instant duSyncedAt,
        JsonNode duProfile
) {
}
