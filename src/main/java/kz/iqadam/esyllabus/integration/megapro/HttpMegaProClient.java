package kz.iqadam.esyllabus.integration.megapro;

import java.util.List;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

final class HttpMegaProClient implements MegaProClient {

    private static final ParameterizedTypeReference<List<MegaProBook>> BOOK_LIST =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;
    private final MegaProProperties properties;

    HttpMegaProClient(RestClient restClient, MegaProProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    @Override
    public List<MegaProBook> searchBooks(String query, int limit) {
        var books = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(properties.searchPath())
                        .queryParam("query", query)
                        .queryParam("limit", limit)
                        .build())
                .retrieve()
                .body(BOOK_LIST);

        return books == null ? List.of() : books;
    }
}
