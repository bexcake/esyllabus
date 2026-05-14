package kz.iqadam.esyllabus.directory.api;

public record StaffPickerOptionResponse(
        String username,
        String fullName,
        String email,
        String positionTitle,
        String schoolId,
        String schoolName,
        String role
) {
}
