package kz.iqadam.esyllabus.security;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import kz.iqadam.esyllabus.config.ApplicationSecurityProperties;
import kz.iqadam.esyllabus.integration.digital.DigitalUniversityRoleClient;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserAccessServiceTests {

    @Test
    void authorizesTeacherFromDigitalUniversity() {
        var service = new UserAccessService(properties(), email -> Set.of("teacher", "methodist"));

        var user = service.authorize(Map.of(
                "preferred_username", "teacher@university.edu",
                "name", "Teacher User"
        ));

        assertThat(user.email()).isEqualTo("teacher@university.edu");
        assertThat(user.displayName()).isEqualTo("Teacher User");
        assertThat(user.roles()).containsExactlyInAnyOrder("TEACHER", "METHODIST");
    }

    @Test
    void rejectsUserWithoutAllowedRole() {
        var service = new UserAccessService(properties(), email -> Set.of("student"));

        assertThatThrownBy(() -> service.authorize(Map.of("preferred_username", "student@university.edu")))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("not allowed");
    }

    private ApplicationSecurityProperties properties() {
        return new ApplicationSecurityProperties(
                List.of("teacher", "professor", "director"),
                URI.create("http://localhost:8080/api/auth/me")
        );
    }
}
