package kz.iqadam.esyllabus.syllabus.api;

public record MySyllabusCardResponse(
        String id,
        String courseId,
        String title,
        String code,
        String program,
        String updatedAt,
        String status,
        int progress,
        int sectionsCompleted,
        int sectionsTotal
) {
}
