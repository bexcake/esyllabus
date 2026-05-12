package kz.iqadam.esyllabus.syllabus.api;

import java.util.List;

public record SyllabusReviewQueueItemResponse(
        String id,
        String courseId,
        String title,
        String code,
        String program,
        String ownerEmail,
        String directorUsername,
        String updatedAt,
        String status,
        List<SyllabusReviewerResponse> colleagueApprovals
) {
}
