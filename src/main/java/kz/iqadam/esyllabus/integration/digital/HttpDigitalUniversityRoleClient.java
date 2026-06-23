package kz.iqadam.esyllabus.integration.digital;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

final class HttpDigitalUniversityRoleClient implements DigitalUniversityRoleClient {

    private static final ParameterizedTypeReference<List<String>> STRING_LIST = new ParameterizedTypeReference<>() {
    };

    private final RestClient restClient;
    private final DigitalUniversityProperties properties;

    HttpDigitalUniversityRoleClient(RestClient restClient, DigitalUniversityProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    @Override
    public Set<String> getRolesByEmail(String email) {
        return Set.of();
    }
}
