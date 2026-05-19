package kz.iqadam.esyllabus.syllabus.service;

import java.util.List;

public record SyllabusMetrics(
        int progress,
        int sectionsCompleted,
        int sectionsTotal,
        List<MissingSection> missingSections
) {

    public boolean readyForReview() {
        return sectionsTotal > 0 && sectionsCompleted == sectionsTotal && missingSections.isEmpty();
    }

    public record MissingSection(
            String key,
            String label,
            String hint
    ) {
    }
}
