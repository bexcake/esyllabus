package kz.iqadam.esyllabus.security;

import java.util.Optional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class DigitalUniversityBearerTokenResolver {

    public Optional<String> currentToken() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        if (authentication.getCredentials() instanceof String token && !token.isBlank()) {
            return Optional.of(token);
        }
        return Optional.empty();
    }
}
