package kz.iqadam.esyllabus.web;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import kz.iqadam.esyllabus.security.AuthenticatedUser;
import kz.iqadam.esyllabus.security.UserAccessService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final UserAccessService userAccessService;

    public AuthController(UserAccessService userAccessService) {
        this.userAccessService = userAccessService;
    }

    @GetMapping("/public/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    @GetMapping("/auth/me")
    public AuthenticatedUser me(Authentication authentication) {
        var principal = (OAuth2User) authentication.getPrincipal();
        var user = userAccessService.authorize(principal.getAttributes());
        var resolvedRoles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        return new AuthenticatedUser(user.email(), user.displayName(), resolvedRoles);
    }

    @GetMapping("/auth/access-denied")
    public ResponseEntity<Map<String, Object>> accessDenied(Authentication authentication) {
        var email = authentication == null ? null : authentication.getName();

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of(
                        "message", "Access is allowed only for teachers, professors, and directors",
                        "email", Objects.toString(email, "anonymous")
                ));
    }

    @GetMapping("/")
    public Map<String, String> home() {
        return Map.of(
                "message", "ESyllabus authentication service is running",
                "loginUrl", "/oauth2/authorization/microsoft"
        );
    }
}
