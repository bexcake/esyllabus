package kz.iqadam.esyllabus.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import kz.iqadam.esyllabus.directory.model.StaffRole;
import kz.iqadam.esyllabus.directory.persistence.SchoolEntity;
import kz.iqadam.esyllabus.directory.persistence.SchoolRepository;
import kz.iqadam.esyllabus.directory.persistence.StaffProfileEntity;
import kz.iqadam.esyllabus.directory.persistence.StaffProfileRepository;
import kz.iqadam.esyllabus.integration.digital.DigitalUniversityBridgeClient;
import kz.iqadam.esyllabus.integration.digital.DigitalUniversityProperties;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DigitalUniversityUserProvisioningService {

    private static final Set<String> DIRECTOR_MARKERS = Set.of(
            "director", "dean", "head", "school_head", "директор", "декан", "руководитель", "меңгеруші"
    );
    private static final Set<String> LIBRARIAN_MARKERS = Set.of(
            "librarian", "library", "библиотек", "кітапхан"
    );
    private static final Set<String> TEACHER_MARKERS = Set.of(
            "teacher", "lecturer", "professor", "instructor", "преподав", "лектор", "профессор", "оқытушы"
    );

    private final DigitalUniversityBridgeClient digitalUniversityBridgeClient;
    private final DigitalUniversityProperties properties;
    private final StaffProfileRepository staffProfileRepository;
    private final SchoolRepository schoolRepository;
    private final ObjectMapper objectMapper;

    public DigitalUniversityUserProvisioningService(
            DigitalUniversityBridgeClient digitalUniversityBridgeClient,
            DigitalUniversityProperties properties,
            StaffProfileRepository staffProfileRepository,
            SchoolRepository schoolRepository,
            ObjectMapper objectMapper
    ) {
        this.digitalUniversityBridgeClient = digitalUniversityBridgeClient;
        this.properties = properties;
        this.staffProfileRepository = staffProfileRepository;
        this.schoolRepository = schoolRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AuthenticatedUser provision(DigitalUniversityJwtClaims claims, String bearerToken) {
        if (!properties.enabled()) {
            var role = claims.roles().isEmpty() ? "TEACHER" : claims.roles().iterator().next();
            return new AuthenticatedUser(
                    claims.principal(),
                    claims.displayName(),
                    Set.of(RoleNormalizer.normalizeRole(role)),
                    null,
                    claims.userId(),
                    claims.principal(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    objectMapper.createObjectNode()
            );
        }

        var existing = findExistingProfile(claims);
        if (existing.isPresent()) {
            var profile = existing.get();
            var raw = profile.getDuRawJson() == null ? objectMapper.createObjectNode() : readRawJson(profile.getDuRawJson());
            return toAuthenticatedUser(profile, raw);
        }

        return fallbackAuthenticatedUser(claims);
    }

    private Optional<StaffProfileEntity> findExistingProfile(DigitalUniversityJwtClaims claims) {
        return staffProfileRepository.findByDuUserId(claims.userId())
                .or(() -> staffProfileRepository.findByEmailIgnoreCase(claims.principal()));
    }

    private AuthenticatedUser fallbackAuthenticatedUser(DigitalUniversityJwtClaims claims) {
        var roles = claims.roles().isEmpty()
                ? Set.of("TEACHER")
                : claims.roles().stream()
                .map(RoleNormalizer::normalizeRole)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        return new AuthenticatedUser(
                claims.principal(),
                claims.displayName(),
                roles,
                null,
                claims.userId(),
                claims.principal(),
                null,
                null,
                null,
                null,
                null,
                objectMapper.createObjectNode()
        );
    }

    @Transactional
    public StaffProfileEntity upsertEmployee(JsonNode employee, String bearerToken, Instant cacheExpiresAt, Instant syncedAt) {
        var employeeId = requiredEmployeeId(employee);
        var userId = userId(employee);
        var email = normalized(employee.path("email").asText(null));
        var username = email == null ? "du-employee-" + employeeId : email;

        var entity = staffProfileRepository.findByDuEmployeeId(employeeId)
                .or(() -> userId == null ? java.util.Optional.empty() : staffProfileRepository.findByDuUserId(userId))
                .or(() -> email == null ? java.util.Optional.empty() : staffProfileRepository.findByEmailIgnoreCase(email))
                .orElseGet(StaffProfileEntity::new);

        if (entity.getId() == null || entity.getId().isBlank()) {
            entity.setId("du-employee-" + employeeId);
        }

        var schoolId = schoolId(employee);
        ensureSchool(employee.path("school"), schoolId);

        entity.setUsername(username);
        entity.setFullName(fullName(employee, username));
        entity.setEmail(email == null ? username : email);
        entity.setWorkplace(firstNonBlank(namedRef(employee.path("department")), namedRef(employee.path("school"))));
        entity.setCabinet(entity.getCabinet());
        entity.setPositionTitle(namedRef(employee.path("position")));
        entity.setSchoolId(schoolId);
        entity.setRole(resolveRole(employee, bearerToken, username, entity.getRole()));
        entity.setDuEmployeeId(employeeId);
        entity.setDuUserId(userId);
        entity.setDuRawJson(writeJson(employee));
        entity.setDuSyncedAt(syncedAt);
        entity.setDuCacheExpiresAt(cacheExpiresAt);
        return staffProfileRepository.save(entity);
    }

    public AuthenticatedUser toAuthenticatedUser(StaffProfileEntity profile, JsonNode employee) {
        return new AuthenticatedUser(
                profile.getEmail(),
                profile.getFullName(),
                Set.of(appRole(profile.getRole())),
                profile.getDuEmployeeId(),
                profile.getDuUserId(),
                profile.getUsername(),
                profile.getSchoolId(),
                schoolRepository.findById(profile.getSchoolId()).map(SchoolEntity::getName).orElse(profile.getSchoolId()),
                profile.getPositionTitle(),
                namedRef(employee.path("status")),
                profile.getDuSyncedAt(),
                employee
        );
    }

    private StaffRole resolveRole(JsonNode employee, String bearerToken, String username, StaffRole existingRole) {
        var employeeId = employeeId(employee);
        var schoolId = schoolId(employee);
        if (employeeId != null && isSchoolHead(employeeId, schoolId, username, bearerToken)) {
            return StaffRole.SCHOOL_DIRECTOR;
        }

        var position = searchable(namedRef(employee.path("position")));
        if (containsAny(position, LIBRARIAN_MARKERS)) {
            return StaffRole.LIBRARIAN;
        }
        if (containsAny(position, DIRECTOR_MARKERS)) {
            return StaffRole.SCHOOL_DIRECTOR;
        }
        if (containsAny(position, TEACHER_MARKERS)) {
            return StaffRole.TEACHER;
        }
        return existingRole == null ? StaffRole.TEACHER : existingRole;
    }

    private boolean isSchoolHead(Long employeeId, String schoolId, String username, String bearerToken) {
        var school = schoolRepository.findById(schoolId);
        if (school.isPresent() && school.get().getDirectorUsername() != null) {
            var directorUsername = school.get().getDirectorUsername();
            if (directorUsername.equalsIgnoreCase(username)
                    || directorUsername.equalsIgnoreCase("du-employee-" + employeeId)) {
                return true;
            }
            if (!directorUsername.startsWith("du-school-")) {
                return false;
            }
        }

        var numericSchoolId = integerValue(schoolId);
        if (numericSchoolId == null) {
            return false;
        }
        var response = digitalUniversityBridgeClient.getSchools(numericSchoolId, bearerToken);
        var schoolItems = items(response);
        for (var item : schoolItems) {
            upsertSchool(item);
            var headEmployeeId = longValue(item.path("schoolHead").path("employeeId"));
            if (employeeId.equals(headEmployeeId)) {
                return true;
            }
        }
        return false;
    }

    public void upsertSchool(JsonNode item) {
        var id = longValue(item.path("id"));
        if (id == null) {
            return;
        }
        var schoolId = String.valueOf(id);
        var entity = schoolRepository.findById(schoolId).orElseGet(SchoolEntity::new);
        entity.setId(schoolId);
        entity.setCode(defaulted("DU-" + id, entity.getCode()));
        entity.setName(firstNonBlank(
                normalized(item.path("schoolNameEn").asText(null)),
                normalized(item.path("schoolNameRu").asText(null)),
                normalized(item.path("schoolNameKk").asText(null)),
                "School " + id
        ));
        var head = item.path("schoolHead");
        var headEmployeeId = longValue(head.path("employeeId"));
        entity.setDirectorUsername(firstNonBlank(
                normalized(head.path("email").asText(null)),
                headEmployeeId == null ? null : "du-employee-" + headEmployeeId,
                "du-school-" + id + "-director"
        ));
        schoolRepository.save(entity);
    }

    private void ensureSchool(JsonNode schoolRef, String schoolId) {
        if (schoolRepository.existsById(schoolId)) {
            return;
        }
        var entity = new SchoolEntity();
        entity.setId(schoolId);
        entity.setCode("DU-" + schoolId);
        entity.setName(defaulted(namedRef(schoolRef), "School " + schoolId));
        entity.setDirectorUsername("du-school-" + schoolId + "-director");
        schoolRepository.save(entity);
    }

    private List<JsonNode> items(JsonNode response) {
        if (response == null || response.isNull() || response.isMissingNode()) {
            return List.of();
        }
        var array = response.path("items");
        if (!array.isArray()) {
            array = response.path("data");
        }
        if (!array.isArray() && response.isArray()) {
            array = response;
        }
        var result = new java.util.ArrayList<JsonNode>();
        if (array.isArray()) {
            array.forEach(result::add);
        }
        return result;
    }

    private String appRole(StaffRole role) {
        return switch (role) {
            case SCHOOL_DIRECTOR -> "DIRECTOR";
            case LIBRARIAN -> "LIBRARIAN";
            case LECTURER, TEACHER -> "TEACHER";
        };
    }

    private String schoolId(JsonNode employee) {
        var schoolId = longValue(employee.path("school").path("id"));
        return schoolId == null ? "du-school-unknown" : String.valueOf(schoolId);
    }

    private String fullName(JsonNode employee, String fallback) {
        var parts = new LinkedHashSet<String>();
        addIfPresent(parts, employee.path("lastName").asText(null));
        addIfPresent(parts, employee.path("firstName").asText(null));
        addIfPresent(parts, employee.path("patronymic").asText(null));
        var value = String.join(" ", parts);
        return value.isBlank() ? fallback : value;
    }

    private void addIfPresent(Set<String> values, String value) {
        var normalized = normalized(value);
        if (normalized != null) {
            values.add(normalized);
        }
    }

    private JsonNode readRawJson(String rawJson) {
        try {
            return objectMapper.readTree(rawJson);
        } catch (JsonProcessingException exception) {
            throw new BadCredentialsException("Cached Digital University employee profile is invalid", exception);
        }
    }

    private String writeJson(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to cache Digital University employee profile", exception);
        }
    }

    private Long requiredEmployeeId(JsonNode employee) {
        var value = employeeId(employee);
        if (value == null) {
            throw new BadCredentialsException("Digital University employee employeeId is missing");
        }
        return value;
    }

    private Long employeeId(JsonNode employee) {
        return firstLong(
                employee.path("employeeId"),
                employee.path("employee").path("id"),
                employee.path("id")
        );
    }

    private Long userId(JsonNode employee) {
        return firstLong(
                employee.path("userId"),
                employee.path("user").path("id")
        );
    }

    private Long firstLong(JsonNode... nodes) {
        for (var node : nodes) {
            var value = longValue(node);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private Long longValue(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return node.longValue();
        }
        var text = normalized(node.asText(null));
        if (text == null) {
            return null;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Integer integerValue(String value) {
        var normalized = normalized(value);
        if (normalized == null) {
            return null;
        }
        try {
            return Integer.parseInt(normalized);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String namedRef(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        return firstNonBlank(
                normalized(node.path("nameEn").asText(null)),
                normalized(node.path("nameRu").asText(null)),
                normalized(node.path("nameKk").asText(null)),
                normalized(node.path("en").asText(null)),
                normalized(node.path("ru").asText(null)),
                normalized(node.path("kk").asText(null)),
                normalized(node.path("name").asText(null))
        );
    }

    private boolean containsAny(String value, Set<String> markers) {
        return markers.stream().anyMatch(value::contains);
    }

    private String searchable(String value) {
        return Objects.toString(value, "").toLowerCase(Locale.ROOT);
    }

    private String defaulted(String value, String fallback) {
        var normalized = normalized(value);
        return normalized == null ? fallback : normalized;
    }

    private String firstNonBlank(String... values) {
        for (var value : values) {
            var normalized = normalized(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private String normalized(String value) {
        if (value == null) {
            return null;
        }
        var result = value.trim();
        return result.isBlank() ? null : result;
    }
}
