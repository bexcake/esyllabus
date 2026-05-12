package kz.iqadam.esyllabus.syllabus.model;

public enum SyllabusStatus {
    DRAFT("Draft"),
    PENDING_COLLEAGUE_CONFIRMATION("Pending Colleague Confirmation"),
    PENDING_DIRECTOR_REVIEW("Pending Director Review"),
    NEEDS_REVIEW("Pending Director Review"),
    PUBLISHED("Published");

    private final String frontendValue;

    SyllabusStatus(String frontendValue) {
        this.frontendValue = frontendValue;
    }

    public String frontendValue() {
        return frontendValue;
    }

    public boolean isPendingDirectorReview() {
        return this == PENDING_DIRECTOR_REVIEW || this == NEEDS_REVIEW;
    }
}
