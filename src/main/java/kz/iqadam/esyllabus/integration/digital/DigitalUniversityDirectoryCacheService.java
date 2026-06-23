package kz.iqadam.esyllabus.integration.digital;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.IntFunction;
import kz.iqadam.esyllabus.integration.digital.persistence.DigitalUniversityCacheEntryEntity;
import kz.iqadam.esyllabus.integration.digital.persistence.DigitalUniversityCacheEntryRepository;
import kz.iqadam.esyllabus.integration.digital.persistence.DigitalUniversityProgramEntity;
import kz.iqadam.esyllabus.integration.digital.persistence.DigitalUniversityProgramRepository;
import kz.iqadam.esyllabus.security.DigitalUniversityUserProvisioningService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DigitalUniversityDirectoryCacheService {

    private static final String REFERENCE_DATA_CACHE_KEY = "reference-data";
    private static final String SCHOOLS_CACHE_KEY = "schools";
    private static final String EMPLOYEES_CACHE_KEY = "employees";
    private static final String PROGRAMS_CACHE_KEY = "education-programs";
    private static final String TEACHER_DISCIPLINES_CACHE_KEY = "teacher-disciplines";

    private final DigitalUniversityBridgeClient digitalUniversityBridgeClient;
    private final DigitalUniversityProperties properties;
    private final DigitalUniversityCacheEntryRepository cacheEntryRepository;
    private final DigitalUniversityProgramRepository programRepository;
    private final DigitalUniversityUserProvisioningService userProvisioningService;
    private final ObjectMapper objectMapper;

    public DigitalUniversityDirectoryCacheService(
            DigitalUniversityBridgeClient digitalUniversityBridgeClient,
            DigitalUniversityProperties properties,
            DigitalUniversityCacheEntryRepository cacheEntryRepository,
            DigitalUniversityProgramRepository programRepository,
            DigitalUniversityUserProvisioningService userProvisioningService,
            ObjectMapper objectMapper
    ) {
        this.digitalUniversityBridgeClient = digitalUniversityBridgeClient;
        this.properties = properties;
        this.cacheEntryRepository = cacheEntryRepository;
        this.programRepository = programRepository;
        this.userProvisioningService = userProvisioningService;
        this.objectMapper = objectMapper;
    }

    @Scheduled(cron = "${digital-university.cache.refresh-cron:0 0 */12 * * *}")
    public void refreshFromServiceTokenIfConfigured() {
        var serviceToken = normalized(properties.serviceToken());
        if (serviceToken != null) {
            refreshReferenceDataIfStale(serviceToken);
        }
    }

    @Transactional
    public synchronized void refreshReferenceDataIfStale(String bearerToken) {
        if (!cacheEnabled() || normalized(bearerToken) == null) {
            return;
        }
        var now = Instant.now();
        var marker = cacheEntryRepository.findById(REFERENCE_DATA_CACHE_KEY);
        if (marker.isPresent() && marker.get().getExpiresAt().isAfter(now)) {
            return;
        }
        refreshReferenceData(bearerToken.trim(), now);
    }

    @Transactional
    public synchronized void refreshReferenceData(String bearerToken, Instant now) {
        var expiresAt = now.plus(refreshInterval());
        var pageSize = pageSize();
        var maxPages = maxPages();

        var schools = items(digitalUniversityBridgeClient.getSchools(null, bearerToken));
        schools.forEach(userProvisioningService::upsertSchool);
        cache(SCHOOLS_CACHE_KEY, schools, now, expiresAt);

        var programs = items(digitalUniversityBridgeClient.getEducationPrograms(null, bearerToken));
        programs.forEach(program -> upsertProgram(program, now));
        cache(PROGRAMS_CACHE_KEY, programs, now, expiresAt);

        var employees = collectPaged(
                page -> digitalUniversityBridgeClient.getEmployees(null, page, pageSize, bearerToken),
                pageSize,
                maxPages
        );
        employees.forEach(employee -> userProvisioningService.upsertEmployee(employee, bearerToken, expiresAt, now));
        cache(EMPLOYEES_CACHE_KEY, employees, now, expiresAt);

        var teacherDisciplines = collectPaged(
                page -> digitalUniversityBridgeClient.getTeacherDisciplines(null, null, null, null, page, pageSize, bearerToken),
                pageSize,
                maxPages
        );
        cache(TEACHER_DISCIPLINES_CACHE_KEY, teacherDisciplines, now, expiresAt);

        cache(REFERENCE_DATA_CACHE_KEY, List.of(), now, expiresAt);
    }

    private void upsertProgram(JsonNode item, Instant now) {
        var externalProgramId = longValue(item.path("id"));
        if (externalProgramId == null) {
            return;
        }
        var entity = programRepository.findByExternalProgramId(externalProgramId)
                .orElseGet(DigitalUniversityProgramEntity::new);
        entity.setId("du-program-" + externalProgramId);
        entity.setExternalProgramId(externalProgramId);
        entity.setCode(normalized(item.path("code").asText(null)));
        entity.setName(firstNonBlank(
                normalized(item.path("programNameEn").asText(null)),
                normalized(item.path("programNameRu").asText(null)),
                normalized(item.path("programNameKk").asText(null)),
                "Program " + externalProgramId
        ));
        var schoolId = longValue(item.path("schoolId"));
        entity.setSchoolId(schoolId == null ? null : String.valueOf(schoolId));
        entity.setActive(!item.has("status") || item.path("status").asBoolean(true));
        entity.setRawJson(writeJson(item));
        entity.setSyncedAt(now);
        programRepository.save(entity);
    }

    private List<JsonNode> collectPaged(IntFunction<JsonNode> fetchPage, int pageSize, int maxPages) {
        var result = new ArrayList<JsonNode>();
        for (var page = 0; page < maxPages; page++) {
            var response = fetchPage.apply(page);
            var pageItems = items(response);
            result.addAll(pageItems);

            var totalPages = integerValue(response.path("totalPages"));
            if (totalPages != null && page + 1 >= totalPages) {
                break;
            }
            if (totalPages == null && pageItems.size() < pageSize) {
                break;
            }
        }
        return result;
    }

    private List<JsonNode> items(JsonNode response) {
        if (response == null || response.isMissingNode() || response.isNull()) {
            return List.of();
        }
        var array = response.path("items");
        if (!array.isArray()) {
            array = response.path("data");
        }
        if (!array.isArray() && response.isArray()) {
            array = response;
        }
        if (!array.isArray()) {
            return List.of();
        }
        var result = new ArrayList<JsonNode>();
        array.forEach(result::add);
        return result;
    }

    private void cache(String key, List<JsonNode> items, Instant refreshedAt, Instant expiresAt) {
        var entity = cacheEntryRepository.findById(key).orElseGet(DigitalUniversityCacheEntryEntity::new);
        entity.setId(key);
        entity.setPayloadJson(writeJson(array(items)));
        entity.setRefreshedAt(refreshedAt);
        entity.setExpiresAt(expiresAt);
        cacheEntryRepository.save(entity);
    }

    private ArrayNode array(List<JsonNode> items) {
        var array = objectMapper.createArrayNode();
        items.forEach(array::add);
        return array;
    }

    private boolean cacheEnabled() {
        return properties.enabled() && properties.cache() != null && properties.cache().enabled();
    }

    private Duration refreshInterval() {
        var value = properties.cache() == null ? null : properties.cache().refreshInterval();
        return value == null ? Duration.ofHours(12) : value;
    }

    private int pageSize() {
        var value = properties.cache() == null ? 100 : properties.cache().pageSize();
        return Math.max(1, value);
    }

    private int maxPages() {
        var value = properties.cache() == null ? 50 : properties.cache().maxPages();
        return Math.max(1, value);
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

    private Integer integerValue(JsonNode node) {
        var value = longValue(node);
        if (value == null || value > Integer.MAX_VALUE || value < Integer.MIN_VALUE) {
            return null;
        }
        return value.intValue();
    }

    private String writeJson(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to cache Digital University payload", exception);
        }
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
        var result = Objects.toString(value, "").trim();
        return result.isBlank() ? null : result;
    }
}
