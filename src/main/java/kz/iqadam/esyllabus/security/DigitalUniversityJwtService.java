package kz.iqadam.esyllabus.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import kz.iqadam.esyllabus.integration.digital.DigitalUniversityProperties;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

@Service
public class DigitalUniversityJwtService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final DigitalUniversityProperties properties;

    public DigitalUniversityJwtService(ObjectMapper objectMapper, DigitalUniversityProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public boolean isEnabled() {
        return properties.jwt() != null && properties.jwt().enabled();
    }

    public DigitalUniversityJwtClaims verify(String token) {
        if (!isEnabled()) {
            throw new BadCredentialsException("Digital University JWT authentication is disabled");
        }
        var secret = normalized(properties.jwt().secret());
        if (secret == null) {
            throw new IllegalStateException("digital-university.jwt.secret must be configured when JWT auth is enabled");
        }

        var parts = token.split("\\.");
        if (parts.length != 3) {
            throw new BadCredentialsException("Invalid JWT format");
        }

        var header = readJson(parts[0], "JWT header");
        var claims = readJson(parts[1], "JWT claims");
        var algorithm = Objects.toString(header.get("alg"), "");
        if (!Set.of("HS256", "HS384", "HS512").contains(algorithm)) {
            throw new BadCredentialsException("Unsupported JWT algorithm");
        }

        verifySignature(parts[0] + "." + parts[1], parts[2], algorithm, secret);
        verifyTimeClaims(claims);

        var principal = firstTextClaim(claims, properties.jwt().principalClaims());
        if (principal == null) {
            throw new BadCredentialsException("JWT principal claim is missing");
        }

        var displayName = firstTextClaim(claims, properties.jwt().displayNameClaims());
        var roles = extractRoles(claims, properties.jwt().roleClaims());
        return new DigitalUniversityJwtClaims(
                principal,
                displayName == null ? principal : displayName,
                roles,
                Map.copyOf(claims)
        );
    }

    private Map<String, Object> readJson(String base64UrlValue, String label) {
        try {
            var bytes = Base64.getUrlDecoder().decode(base64UrlValue);
            return objectMapper.readValue(bytes, MAP_TYPE);
        } catch (Exception exception) {
            throw new BadCredentialsException("Invalid " + label, exception);
        }
    }

    private void verifySignature(String signingInput, String signature, String algorithm, String secret) {
        try {
            var mac = Mac.getInstance(hmacAlgorithm(algorithm));
            mac.init(new SecretKeySpec(secretBytes(secret), mac.getAlgorithm()));
            var expected = mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8));
            var actual = Base64.getUrlDecoder().decode(signature);
            if (!MessageDigest.isEqual(expected, actual)) {
                throw new BadCredentialsException("Invalid JWT signature");
            }
        } catch (BadCredentialsException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BadCredentialsException("Unable to verify JWT signature", exception);
        }
    }

    private String hmacAlgorithm(String jwtAlgorithm) {
        return switch (jwtAlgorithm) {
            case "HS256" -> "HmacSHA256";
            case "HS384" -> "HmacSHA384";
            case "HS512" -> "HmacSHA512";
            default -> throw new BadCredentialsException("Unsupported JWT algorithm");
        };
    }

    private byte[] secretBytes(String secret) {
        try {
            return Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException ignored) {
            return secret.getBytes(StandardCharsets.UTF_8);
        }
    }

    private void verifyTimeClaims(Map<String, Object> claims) {
        var now = Instant.now();
        var clockSkew = properties.jwt().clockSkew();
        var skewSeconds = clockSkew == null ? 0 : Math.max(0, clockSkew.toSeconds());

        var expiresAt = epochSeconds(claims.get("exp"));
        if (expiresAt != null && now.isAfter(Instant.ofEpochSecond(expiresAt + skewSeconds))) {
            throw new BadCredentialsException("JWT has expired");
        }

        var notBefore = epochSeconds(claims.get("nbf"));
        if (notBefore != null && now.plusSeconds(skewSeconds).isBefore(Instant.ofEpochSecond(notBefore))) {
            throw new BadCredentialsException("JWT is not active yet");
        }
    }

    private Long epochSeconds(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Long.parseLong(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String firstTextClaim(Map<String, Object> claims, List<String> claimNames) {
        for (var claimName : safeList(claimNames)) {
            var value = claims.get(claimName);
            if (value instanceof String text) {
                var normalized = normalized(text);
                if (normalized != null) {
                    return normalized;
                }
            }
        }
        return null;
    }

    private Set<String> extractRoles(Map<String, Object> claims, List<String> claimNames) {
        var values = new ArrayList<String>();
        for (var claimName : safeList(claimNames)) {
            collectClaimValues(claims.get(claimName), values);
        }

        return values.stream()
                .map(this::mapDigitalUniversityRole)
                .map(RoleNormalizer::normalizeRole)
                .filter(role -> !role.isBlank())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private void collectClaimValues(Object value, List<String> result) {
        if (value instanceof String text) {
            for (var token : text.split("[,\\s]+")) {
                var normalized = normalized(token);
                if (normalized != null) {
                    result.add(normalized);
                }
            }
            return;
        }
        if (value instanceof Collection<?> collection) {
            for (var item : collection) {
                collectClaimValues(item, result);
            }
        }
    }

    private String mapDigitalUniversityRole(String role) {
        var normalized = RoleNormalizer.normalizeRole(role);
        var compact = normalized.toLowerCase(Locale.ROOT);
        if (compact.contains("librarian") || compact.contains("library")) {
            return "LIBRARIAN";
        }
        if (compact.contains("director") || compact.contains("dean") || compact.contains("school_head")) {
            return "DIRECTOR";
        }
        if (compact.contains("teacher") || compact.contains("lecturer") || compact.contains("instructor")
                || compact.contains("professor")) {
            return "TEACHER";
        }
        return normalized;
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values;
    }

    private String normalized(String value) {
        if (value == null) {
            return null;
        }
        var result = value.trim();
        return result.isBlank() ? null : result;
    }
}
