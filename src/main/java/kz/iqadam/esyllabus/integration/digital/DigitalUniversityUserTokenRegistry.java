package kz.iqadam.esyllabus.integration.digital;

import java.time.Instant;
import java.util.Optional;
import kz.iqadam.esyllabus.security.DigitalUniversityJwtClaims;
import org.springframework.stereotype.Component;

@Component
public class DigitalUniversityUserTokenRegistry {

    private static final long EXPIRY_SAFETY_WINDOW_SECONDS = 60;

    private volatile TokenSnapshot latestToken;

    public void remember(String bearerToken, DigitalUniversityJwtClaims claims) {
        var normalized = normalized(bearerToken);
        if (normalized == null) {
            return;
        }
        latestToken = new TokenSnapshot(normalized, claims.expiresAt());
    }

    public Optional<String> currentToken() {
        var snapshot = latestToken;
        if (snapshot == null) {
            return Optional.empty();
        }
        if (!snapshot.isUsable()) {
            latestToken = null;
            return Optional.empty();
        }
        return Optional.of(snapshot.bearerToken());
    }

    private String normalized(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private record TokenSnapshot(String bearerToken, Instant expiresAt) {

        boolean isUsable() {
            return expiresAt == null
                    || expiresAt.isAfter(Instant.now().plusSeconds(EXPIRY_SAFETY_WINDOW_SECONDS));
        }
    }
}
