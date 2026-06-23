package kz.iqadam.esyllabus.integration.digital;

import com.fasterxml.jackson.databind.JsonNode;
import kz.iqadam.esyllabus.security.DigitalUniversityBearerTokenResolver;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.client.RestClient;

final class HttpDigitalUniversityBridgeClient implements DigitalUniversityBridgeClient {

    private final RestClient restClient;
    private final DigitalUniversityProperties properties;
    private final DigitalUniversityBearerTokenResolver tokenResolver;

    HttpDigitalUniversityBridgeClient(
            RestClient restClient,
            DigitalUniversityProperties properties,
            DigitalUniversityBearerTokenResolver tokenResolver
    ) {
        this.restClient = restClient;
        this.properties = properties;
        this.tokenResolver = tokenResolver;
    }

    @Override
    public JsonNode getStudentByEmail(String email) {
        return get(properties.studentsPath(), email);
    }

    @Override
    public JsonNode getEmployees() {
        return get(properties.employeesPath());
    }

    @Override
    public JsonNode getEmployee(String employeeId) {
        return get(properties.employeePath(), employeeId);
    }

    @Override
    public JsonNode getSchools() {
        return get(properties.schoolsPath());
    }

    @Override
    public JsonNode getEducationPrograms() {
        return get(properties.educationProgramsPath());
    }

    @Override
    public JsonNode getTeacherDisciplines() {
        return get(properties.teacherDisciplinesPath());
    }

    private JsonNode get(String path, Object... uriVariables) {
        var token = tokenResolver.currentToken()
                .orElseThrow(() -> new AccessDeniedException("Digital University bearer token is required"));

        return restClient.get()
                .uri(path, uriVariables)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(JsonNode.class);
    }
}
