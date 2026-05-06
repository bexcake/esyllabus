package kz.iqadam.esyllabus.directory.api;

import java.util.List;

public record StudentResponse(
        String id,
        String username,
        String fullName,
        String email,
        int courseNumber,
        String groupName,
        List<CurrentCourseResponse> currentCourses
) {
    public record CurrentCourseResponse(
            String id,
            String code,
            String title
    ) {
    }
}
