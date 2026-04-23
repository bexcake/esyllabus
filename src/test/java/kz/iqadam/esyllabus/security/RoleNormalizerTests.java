package kz.iqadam.esyllabus.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoleNormalizerTests {

    @Test
    void normalizesRoleNamesToSecurityFormat() {
        assertThat(RoleNormalizer.normalizeRole("teacher assistant"))
                .isEqualTo("TEACHER_ASSISTANT");
        assertThat(RoleNormalizer.toAuthority("director"))
                .isEqualTo("ROLE_DIRECTOR");
    }
}
