package kz.iqadam.esyllabus.directory.api;

public record SchoolResponse(
        String id,
        String code,
        String name,
        String directorUsername,
        String directorName,
        int staffCount
) {
}
