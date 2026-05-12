package kz.iqadam.esyllabus.syllabus.api;

import java.util.List;

public record SyllabusReviewersUpdateRequest(
        List<String> reviewerUsernames
) {
}
