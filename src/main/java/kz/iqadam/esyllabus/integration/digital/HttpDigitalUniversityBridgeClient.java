package kz.iqadam.esyllabus.integration.digital;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.Map;
import kz.iqadam.esyllabus.security.DigitalUniversityBearerTokenResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.client.RestClient;

final class HttpDigitalUniversityBridgeClient implements DigitalUniversityBridgeClient {

    private final RestClient restClient;
    private final DigitalUniversityProperties properties;
    private final DigitalUniversityBearerTokenResolver tokenResolver;
    private final ObjectMapper objectMapper;

    HttpDigitalUniversityBridgeClient(
            RestClient restClient,
            DigitalUniversityProperties properties,
            DigitalUniversityBearerTokenResolver tokenResolver,
            ObjectMapper objectMapper
    ) {
        this.restClient = restClient;
        this.properties = properties;
        this.tokenResolver = tokenResolver;
        this.objectMapper = objectMapper;
    }

    @Override
    public JsonNode getStudentByEmail(String email) {
        return getStudentByEmail(email, currentToken());
    }

    @Override
    public JsonNode getStudentByEmail(String email, String bearerToken) {
        return get(bearerToken, properties.studentPath(), Map.of("studentEmail", email));
    }

    @Override
    public JsonNode getStudents(Integer course, Integer schoolId, Integer programId, int page, int size) {
        return getStudents(course, schoolId, programId, page, size, currentToken());
    }

    @Override
    public JsonNode getStudents(Integer course, Integer schoolId, Integer programId, int page, int size, String bearerToken) {
        return get(
                bearerToken,
                properties.studentsPath(),
                Map.of(),
                queryParam("course", course),
                queryParam("schoolId", schoolId),
                queryParam("programId", programId),
                queryParam("page", normalizedPage(page)),
                queryParam("size", normalizedSize(size))
        );
    }

    @Override
    public JsonNode getEmployees() {
        return getEmployees(null, 0, 50);
    }

    @Override
    public JsonNode getEmployees(Integer schoolId, int page, int size) {
        return getEmployees(schoolId, page, size, currentToken());
    }

    @Override
    public JsonNode getEmployees(Integer schoolId, int page, int size, String bearerToken) {
        return get(
                bearerToken,
                properties.employeesPath(),
                Map.of(),
                queryParam("schoolId", schoolId),
                queryParam("page", normalizedPage(page)),
                queryParam("size", normalizedSize(size))
        );
    }

    @Override
    public JsonNode getEmployee(Integer employeeId) {
        return getEmployee(employeeId, currentToken());
    }

    @Override
    public JsonNode getEmployee(Integer employeeId, String bearerToken) {
        return get(bearerToken, properties.employeePath(), Map.of("employeeId", employeeId));
    }

    @Override
    public JsonNode getSchools() {
        return getSchools(null);
    }

    @Override
    public JsonNode getSchools(Integer schoolId) {
        return getSchools(schoolId, currentToken());
    }

    @Override
    public JsonNode getSchools(Integer schoolId, String bearerToken) {
        return get(bearerToken, properties.schoolsPath(), Map.of(), queryParam("schoolId", schoolId));
    }

    @Override
    public JsonNode getEducationPrograms() {
        return getEducationPrograms(null);
    }

    @Override
    public JsonNode getEducationPrograms(Integer programId) {
        return getEducationPrograms(programId, currentToken());
    }

    @Override
    public JsonNode getEducationPrograms(Integer programId, String bearerToken) {
        return get(bearerToken, properties.educationProgramsPath(), Map.of(), queryParam("programId", programId));
    }

    @Override
    public JsonNode getTeacherDisciplines() {
        return getTeacherDisciplines(null, null, null, null, 0, 50);
    }

    @Override
    public JsonNode getTeacherDisciplines(Integer schoolId, Integer teacherId, Integer academicYear, Integer term, int page, int size) {
        return getTeacherDisciplines(schoolId, teacherId, academicYear, term, page, size, currentToken());
    }

    @Override
    public JsonNode getTeacherDisciplines(
            Integer schoolId,
            Integer teacherId,
            Integer academicYear,
            Integer term,
            int page,
            int size,
            String bearerToken
    ) {
        return get(
                bearerToken,
                properties.teacherDisciplinesPath(),
                Map.of(),
                queryParam("schoolId", schoolId),
                queryParam("teacherId", teacherId),
                queryParam("academicYear", academicYear),
                queryParam("term", term),
                queryParam("page", normalizedPage(page)),
                queryParam("size", normalizedSize(size))
        );
    }

    private String currentToken() {
        return tokenResolver.currentToken()
                .orElseThrow(() -> new AccessDeniedException("Digital University bearer token is required"));
    }

    private JsonNode get(String bearerToken, String path, Map<String, ?> uriVariables, QueryParam... queryParams) {
        if (bearerToken == null || bearerToken.isBlank()) {
            throw new AccessDeniedException("Digital University bearer token is required");
        }
        var response = restClient.get()
                .uri(builder -> uri(path, builder, uriVariables, queryParams))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken.trim())
                .retrieve()
                .body(String.class);
        try {
            return objectMapper.readTree(response == null ? "{}" : response);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Digital University returned invalid JSON");
        }
    }

    private URI uri(String path, UriBuilder builder, Map<String, ?> uriVariables, QueryParam... queryParams) {
        var result = builder.path(path);
        for (var queryParam : queryParams) {
            if (queryParam.value() != null) {
                result.queryParam(queryParam.name(), queryParam.value());
            }
        }
        return result.build(uriVariables);
    }

    private QueryParam queryParam(String name, Object value) {
        return new QueryParam(name, value);
    }

    private int normalizedPage(int page) {
        return Math.max(0, page);
    }

    private int normalizedSize(int size) {
        return Math.max(1, size);
    }

    private record QueryParam(String name, Object value) {
    }
}
