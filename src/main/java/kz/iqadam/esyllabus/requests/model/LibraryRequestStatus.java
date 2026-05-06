package kz.iqadam.esyllabus.requests.model;

public enum LibraryRequestStatus {
    DRAFT("Draft"),
    PENDING_DIRECTOR_APPROVAL("Pending Director Approval"),
    APPROVED_BY_DIRECTOR("Approved by Director"),
    REJECTED_BY_DIRECTOR("Rejected by Director"),
    FEEDBACK_PROVIDED("Feedback Provided");

    private final String frontendValue;

    LibraryRequestStatus(String frontendValue) {
        this.frontendValue = frontendValue;
    }

    public String frontendValue() {
        return frontendValue;
    }
}
