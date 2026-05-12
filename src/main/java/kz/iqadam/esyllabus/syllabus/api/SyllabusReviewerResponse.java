package kz.iqadam.esyllabus.syllabus.api;

public record SyllabusReviewerResponse(
        String username,
        String fullName,
        String role,
        boolean approved
) {
}
