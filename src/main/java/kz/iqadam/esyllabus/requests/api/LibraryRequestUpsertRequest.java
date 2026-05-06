package kz.iqadam.esyllabus.requests.api;

import java.time.LocalDate;
import java.util.List;

public record LibraryRequestUpsertRequest(
        String department,
        String educationLevel,
        LocalDate requestDate,
        List<ItemRequest> items
) {
    public record ItemRequest(
            String title,
            String author,
            String isbn,
            String publisher,
            String publicationYear,
            String discipline,
            String educationalProgram,
            Integer courseNumber,
            String trimester,
            Integer quantity,
            String literatureType
    ) {
    }
}
