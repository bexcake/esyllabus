package kz.iqadam.esyllabus.syllabus.api;

public record LibraryBookResponse(
        String externalId,
        String title,
        String author,
        String year,
        String url,
        String type
) {
}
