package kz.iqadam.esyllabus.syllabus.service;

public class SyllabusReviewValidationException extends RuntimeException {

    private final SyllabusMetrics metrics;

    public SyllabusReviewValidationException(String message, SyllabusMetrics metrics) {
        super(message);
        this.metrics = metrics;
    }

    public SyllabusMetrics metrics() {
        return metrics;
    }
}
