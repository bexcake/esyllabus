package kz.iqadam.esyllabus.requests.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record LibraryRequestResponse(
        String id,
        String requesterUsername,
        String syllabusId,
        String requesterName,
        String schoolId,
        String schoolName,
        String directorUsername,
        String department,
        String educationLevel,
        LocalDate requestDate,
        String status,
        String directorComment,
        String libraryFeedback,
        String expectedPurchaseMonth,
        Instant createdAt,
        Instant updatedAt,
        List<ItemResponse> items
) {
    public record ItemResponse(
            int lineNumber,
            String title,
            String author,
            String isbn,
            String publisher,
            String publicationYear,
            String discipline,
            String educationalProgram,
            int courseNumber,
            String trimester,
            int quantity,
            String literatureType
    ) {
    }
}
