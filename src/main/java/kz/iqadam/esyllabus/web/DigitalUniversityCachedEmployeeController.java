package kz.iqadam.esyllabus.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import kz.iqadam.esyllabus.directory.persistence.StaffProfileEntity;
import kz.iqadam.esyllabus.directory.persistence.StaffProfileRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DigitalUniversityCachedEmployeeController {

    private final StaffProfileRepository staffProfileRepository;
    private final ObjectMapper objectMapper;

    public DigitalUniversityCachedEmployeeController(
            StaffProfileRepository staffProfileRepository,
            ObjectMapper objectMapper
    ) {
        this.staffProfileRepository = staffProfileRepository;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/api/v1/user/employees")
    public Map<String, Object> getCachedEmployees(
            @RequestParam(required = false) Integer schoolId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        var normalizedSchoolId = schoolId == null ? null : String.valueOf(schoolId);
        var normalizedSearch = normalized(search);
        var normalizedPage = Math.max(0, page);
        var normalizedSize = Math.max(1, Math.min(size, 500));

        var filtered = staffProfileRepository.findAll().stream()
                .filter(profile -> profile.getDuEmployeeId() != null)
                .filter(profile -> normalizedSchoolId == null || normalizedSchoolId.equals(profile.getSchoolId()))
                .filter(profile -> normalizedSearch == null || searchable(profile).contains(normalizedSearch))
                .sorted(Comparator.comparing(StaffProfileEntity::getFullName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        var fromIndex = Math.min(normalizedPage * normalizedSize, filtered.size());
        var toIndex = Math.min(fromIndex + normalizedSize, filtered.size());
        var data = filtered.subList(fromIndex, toIndex).stream()
                .map(this::toEmployeeJson)
                .toList();
        var totalPages = (int) Math.ceil(filtered.size() / (double) normalizedSize);

        return Map.of(
                "data", data,
                "items", data,
                "page", normalizedPage,
                "size", normalizedSize,
                "totalElements", filtered.size(),
                "totalPages", totalPages
        );
    }

    private JsonNode toEmployeeJson(StaffProfileEntity profile) {
        ObjectNode node = objectMapper.createObjectNode();
        if (profile.getDuRawJson() != null && !profile.getDuRawJson().isBlank()) {
            try {
                var raw = objectMapper.readTree(profile.getDuRawJson());
                if (raw.isObject()) {
                    node = (ObjectNode) raw.deepCopy();
                }
            } catch (Exception ignored) {
                node = objectMapper.createObjectNode();
            }
        }

        putIfMissing(node, "employeeId", profile.getDuEmployeeId());
        putIfMissing(node, "userId", profile.getDuUserId());
        putIfMissing(node, "email", profile.getEmail());
        putIfMissing(node, "fullName", profile.getFullName());
        putIfMissing(node, "schoolId", profile.getSchoolId());
        putIfMissing(node, "positionTitle", profile.getPositionTitle());
        putIfMissing(node, "role", profile.getRole() == null ? null : profile.getRole().name());
        return node;
    }

    private void putIfMissing(ObjectNode node, String fieldName, Long value) {
        if (!node.hasNonNull(fieldName) && value != null) {
            node.put(fieldName, value);
        }
    }

    private void putIfMissing(ObjectNode node, String fieldName, String value) {
        if (!node.hasNonNull(fieldName) && value != null) {
            node.put(fieldName, value);
        }
    }

    private String searchable(StaffProfileEntity profile) {
        return String.join(" ", List.of(
                        Objects.toString(profile.getUsername(), ""),
                        Objects.toString(profile.getFullName(), ""),
                        Objects.toString(profile.getEmail(), ""),
                        Objects.toString(profile.getPositionTitle(), "")
                ))
                .toLowerCase(java.util.Locale.ROOT);
    }

    private String normalized(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
