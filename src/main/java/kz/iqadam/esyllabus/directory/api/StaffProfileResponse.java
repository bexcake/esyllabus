package kz.iqadam.esyllabus.directory.api;

public record StaffProfileResponse(
        String id,
        String username,
        String fullName,
        String email,
        String workplace,
        String cabinet,
        String positionTitle,
        String schoolId,
        String schoolName,
        String role
) {
}
