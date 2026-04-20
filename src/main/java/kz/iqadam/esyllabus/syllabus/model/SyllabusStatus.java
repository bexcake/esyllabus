package kz.iqadam.esyllabus.syllabus.model;

public enum SyllabusStatus {
    DRAFT("Draft"),
    NEEDS_REVIEW("Needs Review"),
    PUBLISHED("Published");

    private final String frontendValue;

    SyllabusStatus(String frontendValue) {
        this.frontendValue = frontendValue;
    }

    public String frontendValue() {
        return frontendValue;
    }
}
