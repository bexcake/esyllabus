package kz.iqadam.esyllabus.security;

import java.util.Locale;

public final class RoleNormalizer {

    private RoleNormalizer() {
    }

    public static String normalizeRole(String role) {
        return role == null
                ? ""
                : role.trim()
                        .replace('-', '_')
                        .replace(' ', '_')
                        .toUpperCase(Locale.ROOT);
    }

    public static String toAuthority(String role) {
        return "ROLE_" + normalizeRole(role);
    }
}
