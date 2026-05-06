package kz.iqadam.esyllabus.requests.api;

public record LibraryRequestFeedbackRequest(
        String feedback,
        String expectedPurchaseMonth
) {
}
