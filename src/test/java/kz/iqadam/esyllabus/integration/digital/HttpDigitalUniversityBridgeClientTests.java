package kz.iqadam.esyllabus.integration.digital;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import com.fasterxml.jackson.databind.ObjectMapper;
import kz.iqadam.esyllabus.security.DigitalUniversityBearerTokenResolver;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class HttpDigitalUniversityBridgeClientTests {

    private static final String BASE_URL = "https://bridge-du.astanait.edu.kz";

    @Test
    void buildsEmployeeListRequestAccordingToSwagger() {
        var builder = RestClient.builder().baseUrl(BASE_URL);
        var server = MockRestServiceServer.bindTo(builder).build();
        var client = new HttpDigitalUniversityBridgeClient(builder.build(), properties(), tokenResolver(), new ObjectMapper());

        server.expect(requestTo(BASE_URL + "/api/v1/user/employees?schoolId=7&page=2&size=25"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer du-token"))
                .andRespond(withSuccess("{\"data\":[]}", MediaType.APPLICATION_JSON));

        var response = client.getEmployees(7, 2, 25);

        assertThat(response.path("data").isArray()).isTrue();
        server.verify();
    }

    @Test
    void buildsTeacherDisciplinesRequestAccordingToSwagger() {
        var builder = RestClient.builder().baseUrl(BASE_URL);
        var server = MockRestServiceServer.bindTo(builder).build();
        var client = new HttpDigitalUniversityBridgeClient(builder.build(), properties(), tokenResolver(), new ObjectMapper());

        server.expect(requestTo(BASE_URL + "/api/v1/teacher_disciplines?schoolId=3&teacherId=42&academicYear=2026&term=1&page=0&size=50"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer du-token"))
                .andRespond(withSuccess("{\"items\":[]}", MediaType.APPLICATION_JSON));

        var response = client.getTeacherDisciplines(3, 42, 2026, 1, 0, 50);

        assertThat(response.path("items").isArray()).isTrue();
        server.verify();
    }

    @Test
    void buildsStudentDetailsRequestAccordingToSwaggerPathVariable() {
        var builder = RestClient.builder().baseUrl(BASE_URL);
        var server = MockRestServiceServer.bindTo(builder).build();
        var client = new HttpDigitalUniversityBridgeClient(builder.build(), properties(), tokenResolver(), new ObjectMapper());

        server.expect(requestTo(BASE_URL + "/api/v1/students/200000%40astanait.edu.kz"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer du-token"))
                .andRespond(withSuccess("{\"email\":\"200000@astanait.edu.kz\"}", MediaType.APPLICATION_JSON));

        var response = client.getStudentByEmail("200000@astanait.edu.kz");

        assertThat(response.path("email").asText()).isEqualTo("200000@astanait.edu.kz");
        server.verify();
    }

    private DigitalUniversityProperties properties() {
        return new DigitalUniversityProperties(
                true,
                URI.create(BASE_URL),
                "/api/v1/students/{studentEmail}",
                "/api/v1/user/students",
                "/api/v1/user/employees",
                "/api/v1/employees/{employeeId}",
                "/api/v1/schools",
                "/api/v1/education_programs",
                "/api/v1/teacher_disciplines",
                Duration.ofSeconds(5),
                Duration.ofSeconds(10),
                new DigitalUniversityProperties.Jwt(
                        true,
                        "test-secret",
                        Duration.ofSeconds(60),
                        List.of("email"),
                        List.of("name"),
                        List.of("roles")
                )
        );
    }

    private DigitalUniversityBearerTokenResolver tokenResolver() {
        return new DigitalUniversityBearerTokenResolver() {
            @Override
            public Optional<String> currentToken() {
                return Optional.of("du-token");
            }
        };
    }
}
