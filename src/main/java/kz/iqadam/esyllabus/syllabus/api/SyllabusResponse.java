package kz.iqadam.esyllabus.syllabus.api;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

public record SyllabusResponse(
        String id,
        String courseId,
        String ownerEmail,
        String status,
        int progress,
        int sectionsCompleted,
        int sectionsTotal,
        String reviewComment,
        Instant updatedAt,
        JsonNode content
) {
}
