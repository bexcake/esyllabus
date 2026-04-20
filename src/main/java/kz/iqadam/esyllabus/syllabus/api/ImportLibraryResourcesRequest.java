package kz.iqadam.esyllabus.syllabus.api;

import java.util.List;

public record ImportLibraryResourcesRequest(
        List<LibraryBookItem> books
) {
    public record LibraryBookItem(
            String title,
            String author,
            String year,
            String type,
            String url
    ) {
    }
}
