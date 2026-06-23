package kz.iqadam.esyllabus.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import kz.iqadam.esyllabus.directory.model.StaffRole;
import kz.iqadam.esyllabus.directory.persistence.StaffProfileRepository;
import kz.iqadam.esyllabus.integration.digital.DigitalUniversityBridgeClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:du-provisioning;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "digital-university.enabled=true",
        "digital-university.cache.enabled=false"
})
class DigitalUniversityUserProvisioningServiceTests {

    @Autowired
    private DigitalUniversityUserProvisioningService provisioningService;

    @Autowired
    private StaffProfileRepository staffProfileRepository;

    @Autowired
    private FakeDigitalUniversityBridgeClient bridgeClient;

    @Test
    void provisionsEmployeeFromJwtUserIdAndAssignsDirectorRoleFromSchoolHead() {
        var claims = new DigitalUniversityJwtClaims(
                "5001",
                "5001",
                5001L,
                Instant.now().plus(Duration.ofMinutes(30)),
                Set.of(),
                Map.of("sub", 5001)
        );

        var user = provisioningService.provision(claims, "du-token");

        assertThat(bridgeClient.employeesToken).isEqualTo("du-token");
        assertThat(bridgeClient.employeeToken).isEqualTo("du-token");
        assertThat(user.email()).isEqualTo("director@astanait.edu.kz");
        assertThat(user.employeeId()).isEqualTo(1001L);
        assertThat(user.userId()).isEqualTo(5001L);
        assertThat(user.roles()).containsExactly("DIRECTOR");
        assertThat(user.schoolId()).isEqualTo("7");
        assertThat(user.duProfile().path("employeeId").asLong()).isEqualTo(1001L);

        var profile = staffProfileRepository.findByDuEmployeeId(1001L).orElseThrow();
        assertThat(profile.getEmail()).isEqualTo("director@astanait.edu.kz");
        assertThat(profile.getRole()).isEqualTo(StaffRole.SCHOOL_DIRECTOR);
        assertThat(profile.getDuRawJson()).contains("\"employeeId\":1001");
        assertThat(profile.getDuCacheExpiresAt()).isAfter(Instant.now());
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        FakeDigitalUniversityBridgeClient fakeDigitalUniversityBridgeClient() {
            return new FakeDigitalUniversityBridgeClient();
        }
    }

    static class FakeDigitalUniversityBridgeClient implements DigitalUniversityBridgeClient {

        private final JsonNodeFactory json = JsonNodeFactory.instance;
        private String employeeToken;
        private String employeesToken;

        @Override
        public JsonNode getStudentByEmail(String email) {
            return json.objectNode();
        }

        @Override
        public JsonNode getStudentByEmail(String email, String bearerToken) {
            return json.objectNode();
        }

        @Override
        public JsonNode getStudents(Integer course, Integer schoolId, Integer programId, int page, int size) {
            return json.objectNode();
        }

        @Override
        public JsonNode getStudents(Integer course, Integer schoolId, Integer programId, int page, int size, String bearerToken) {
            return json.objectNode();
        }

        @Override
        public JsonNode getEmployees() {
            return json.objectNode();
        }

        @Override
        public JsonNode getEmployees(Integer schoolId, int page, int size) {
            return json.objectNode();
        }

        @Override
        public JsonNode getEmployees(Integer schoolId, int page, int size, String bearerToken) {
            employeesToken = bearerToken;
            var response = json.objectNode();
            response.put("totalElements", 1);
            response.put("totalPages", 1);
            var data = response.putArray("data");
            data.add(employee(1001, 5001));
            return response;
        }

        @Override
        public JsonNode getEmployee(Integer employeeId) {
            return employee(employeeId, 5001);
        }

        @Override
        public JsonNode getEmployee(Integer employeeId, String bearerToken) {
            employeeToken = bearerToken;
            return employee(employeeId, 5001);
        }

        @Override
        public JsonNode getSchools() {
            return schools();
        }

        @Override
        public JsonNode getSchools(Integer schoolId) {
            return schools();
        }

        @Override
        public JsonNode getSchools(Integer schoolId, String bearerToken) {
            return schools();
        }

        @Override
        public JsonNode getEducationPrograms() {
            return json.objectNode();
        }

        @Override
        public JsonNode getEducationPrograms(Integer programId) {
            return json.objectNode();
        }

        @Override
        public JsonNode getEducationPrograms(Integer programId, String bearerToken) {
            return json.objectNode();
        }

        @Override
        public JsonNode getTeacherDisciplines() {
            return json.objectNode();
        }

        @Override
        public JsonNode getTeacherDisciplines(Integer schoolId, Integer teacherId, Integer academicYear, Integer term, int page, int size) {
            return json.objectNode();
        }

        @Override
        public JsonNode getTeacherDisciplines(Integer schoolId, Integer teacherId, Integer academicYear, Integer term, int page, int size, String bearerToken) {
            return json.objectNode();
        }

        private JsonNode employee(Integer employeeId, Integer userId) {
            var employee = json.objectNode();
            employee.put("employeeId", employeeId);
            employee.put("userId", userId);
            employee.put("email", "director@astanait.edu.kz");
            employee.put("firstName", "Aitu");
            employee.put("lastName", "Director");
            employee.set("position", namedRef(11, "Director of School"));
            employee.set("department", namedRef(21, "Academic Office"));
            employee.set("school", namedRef(7, "School of Computing"));
            employee.set("status", namedRef(1, "Active"));
            return employee;
        }

        private JsonNode schools() {
            var response = json.objectNode();
            var items = response.putArray("items");
            var school = items.addObject();
            school.put("id", 7);
            school.put("schoolNameEn", "School of Computing");
            school.put("status", true);
            var head = school.putObject("schoolHead");
            head.put("employeeId", 1001);
            head.put("userId", 5001);
            head.put("email", "director@astanait.edu.kz");
            return response;
        }

        private JsonNode namedRef(int id, String nameEn) {
            var node = json.objectNode();
            node.put("id", id);
            node.put("nameEn", nameEn);
            return node;
        }
    }
}
