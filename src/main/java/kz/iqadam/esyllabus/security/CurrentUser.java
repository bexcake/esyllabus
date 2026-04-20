package kz.iqadam.esyllabus.security;

import java.util.Set;

public record CurrentUser(
        String email,
        Set<String> roles
) {

    public boolean hasAnyRole(String... expectedRoles) {
        for (var expectedRole : expectedRoles) {
            if (roles.contains(UserAccessService.normalizeRole(expectedRole))) {
                return true;
            }
        }
        return false;
    }
}
