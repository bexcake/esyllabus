package kz.iqadam.esyllabus.syllabus.api;

public record LibraryBookTagResponse(
        String value,
        String label,
        long booksCount
) {
}
