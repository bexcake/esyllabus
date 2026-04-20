package kz.iqadam.esyllabus.integration.megapro;

public record MegaProBook(
        String externalId,
        String title,
        String author,
        String year,
        String url,
        String type
) {
}
