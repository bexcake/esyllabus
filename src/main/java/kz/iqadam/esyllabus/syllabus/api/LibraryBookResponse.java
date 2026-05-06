package kz.iqadam.esyllabus.syllabus.api;

import java.time.Instant;
import java.util.List;

public record LibraryBookResponse(
        String externalId,
        String title,
        String author,
        String year,
        String url,
        String type,
        String discipline,
        List<String> disciplineTags,
        Instant syncedAt
) {
}
