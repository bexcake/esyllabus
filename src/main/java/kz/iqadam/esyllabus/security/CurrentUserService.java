package kz.iqadam.esyllabus.security;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    public CurrentUser getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authentication is required");
        }

        var email = Objects.toString(authentication.getName(), "").trim();
        if (email.isBlank()) {
            throw new AccessDeniedException("Authenticated email is missing");
        }

        var roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .map(UserAccessService::normalizeRole)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        return new CurrentUser(email, Set.copyOf(roles));
    }
}
