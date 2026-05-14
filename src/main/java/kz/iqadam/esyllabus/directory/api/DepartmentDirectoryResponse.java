package kz.iqadam.esyllabus.directory.api;

public record DepartmentDirectoryResponse(
        String id,
        String name,
        String schoolId,
        String schoolName
) {
}
