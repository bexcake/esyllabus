package kz.iqadam.esyllabus.web;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import kz.iqadam.esyllabus.security.AuthenticatedUser;
import kz.iqadam.esyllabus.security.DigitalUniversityJwtClaims;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthController {

    @GetMapping("/public/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    @GetMapping("/auth/me")
    public AuthenticatedUser me(Authentication authentication) {
        var resolvedRoles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        var displayName = authentication.getDetails() instanceof DigitalUniversityJwtClaims claims
                ? claims.displayName()
                : authentication.getName();

        return new AuthenticatedUser(authentication.getName(), displayName, resolvedRoles);
    }

    @GetMapping("/auth/access-denied")
    public ResponseEntity<Map<String, Object>> accessDenied(Authentication authentication) {
        var email = authentication == null ? null : authentication.getName();

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of(
                        "message", "Access is allowed only for teachers, directors, librarians, and students",
                        "email", Objects.toString(email, "anonymous")
                ));
    }

    @GetMapping("/")
    public Map<String, String> home() {
        return Map.of(
                "message", "ESyllabus service is running",
                "authentication", "Digital University Bearer JWT"
        );
    }
}
