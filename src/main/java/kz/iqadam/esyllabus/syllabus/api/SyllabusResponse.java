package kz.iqadam.esyllabus.syllabus.api;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.List;

public record SyllabusResponse(
        String id,
        String courseId,
        String ownerEmail,
        String directorUsername,
        String status,
        int progress,
        int sectionsCompleted,
        int sectionsTotal,
        String reviewComment,
        List<SyllabusReviewerResponse> colleagueApprovals,
        String linkedLibraryRequestId,
        Instant updatedAt,
        JsonNode content
) {
}
