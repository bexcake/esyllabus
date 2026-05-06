package kz.iqadam.esyllabus.syllabus.api;

import java.time.Instant;
import java.util.List;

public record DisciplineCatalogItemResponse(
        String courseId,
        String courseTitle,
        String code,
        String program,
        String schoolId,
        List<String> disciplineTags,
        int synchronizedBooks,
        Instant lastSyncedAt
) {
}
