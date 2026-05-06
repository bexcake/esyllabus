package kz.iqadam.esyllabus.directory.api;

import java.util.List;

public record StudentResponse(
        String id,
        String fullName,
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
