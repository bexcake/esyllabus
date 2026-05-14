package kz.iqadam.esyllabus.directory.api;

public record ProgramDirectoryResponse(
        String id,
        String code,
        String name,
        String degreeLevel,
        String schoolId,
        String schoolName
) {
}
