package kz.iqadam.esyllabus.syllabus.service;

public record SyllabusMetrics(
        int progress,
        int sectionsCompleted,
        int sectionsTotal
) {

    public boolean readyForReview() {
        return sectionsTotal > 0 && sectionsCompleted == sectionsTotal;
    }
}
