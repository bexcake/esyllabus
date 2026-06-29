package kz.iqadam.esyllabus.integration.digital;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import kz.iqadam.esyllabus.integration.digital.persistence.DigitalUniversityCacheEntryEntity;
import kz.iqadam.esyllabus.integration.digital.persistence.DigitalUniversityCacheEntryRepository;
import kz.iqadam.esyllabus.integration.digital.persistence.DigitalUniversityProgramEntity;
import kz.iqadam.esyllabus.integration.digital.persistence.DigitalUniversityProgramRepository;
import kz.iqadam.esyllabus.security.DigitalUniversityUserProvisioningService;
import kz.iqadam.esyllabus.syllabus.persistence.CourseEntity;
import kz.iqadam.esyllabus.syllabus.persistence.CourseRepository;
import kz.iqadam.esyllabus.syllabus.service.CourseMetadataSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class DigitalUniversityDirectoryCacheService {

    private static final Logger log = LoggerFactory.getLogger(DigitalUniversityDirectoryCacheService.class);

    private static final String REFERENCE_DATA_CACHE_KEY = "reference-data";

    private final DigitalUniversityBridgeClient digitalUniversityBridgeClient;
    private final DigitalUniversityProperties properties;
    private final DigitalUniversityUserTokenRegistry userTokenRegistry;
    private final DigitalUniversityCacheEntryRepository cacheEntryRepository;
    private final DigitalUniversityProgramRepository programRepository;
    private final CourseRepository courseRepository;
    private final DigitalUniversityUserProvisioningService userProvisioningService;
    private final ObjectMapper objectMapper;
    private final TaskExecutor taskExecutor;
    private final TransactionTemplate transactionTemplate;
    private Instant refreshFailureCooldownUntil;
    private boolean refreshInProgress;
    private boolean refreshPending;

    public DigitalUniversityDirectoryCacheService(
            DigitalUniversityBridgeClient digitalUniversityBridgeClient,
            DigitalUniversityProperties properties,
            DigitalUniversityUserTokenRegistry userTokenRegistry,
            DigitalUniversityCacheEntryRepository cacheEntryRepository,
            DigitalUniversityProgramRepository programRepository,
            CourseRepository courseRepository,
            DigitalUniversityUserProvisioningService userProvisioningService,
            ObjectMapper objectMapper,
            @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor,
            TransactionTemplate transactionTemplate
    ) {
        this.digitalUniversityBridgeClient = digitalUniversityBridgeClient;
        this.properties = properties;
        this.userTokenRegistry = userTokenRegistry;
        this.cacheEntryRepository = cacheEntryRepository;
        this.programRepository = programRepository;
        this.courseRepository = courseRepository;
        this.userProvisioningService = userProvisioningService;
        this.objectMapper = objectMapper;
        this.taskExecutor = taskExecutor;
        this.transactionTemplate = transactionTemplate;
    }

    @Scheduled(cron = "${digital-university.cache.refresh-cron:0 0 */12 * * *}")
    public void refreshFromLatestUserTokenIfConfigured() {
        requestRefreshWhenTokenAvailable("scheduled");
    }

    public void requestRefreshWhenTokenAvailable(String trigger) {
        String bearerToken;
        synchronized (this) {
            if (!cacheEnabled()) {
                return;
            }
            var now = Instant.now();
            if (!referenceDataIsStale(now)) {
                refreshPending = false;
                return;
            }
            if (refreshFailureCooldownUntil != null && now.isBefore(refreshFailureCooldownUntil)) {
                refreshPending = true;
                return;
            }
            var token = userTokenRegistry.currentToken();
            if (token.isEmpty()) {
                refreshPending = true;
                if (!"user-token".equals(trigger)) {
                    log.info("digital_university_sync_waiting_for_user_token trigger={}", trigger);
                }
                return;
            }
            if (refreshInProgress) {
                refreshPending = true;
                return;
            }
            bearerToken = token.get();
            refreshInProgress = true;
            refreshPending = false;
        }

        taskExecutor.execute(() -> runRefreshInBackground(bearerToken, trigger));
    }

    private void runRefreshInBackground(String bearerToken, String trigger) {
        try {
            transactionTemplate.executeWithoutResult(status -> refreshReferenceDataIfStale(bearerToken));
            log.info("digital_university_sync_completed trigger={}", trigger);
        } catch (RuntimeException exception) {
            synchronized (this) {
                refreshFailureCooldownUntil = Instant.now().plus(failureCooldown());
                refreshPending = true;
            }
            log.warn("digital_university_sync_failed trigger={} message=\"{}\"", trigger, exception.getMessage());
        } finally {
            synchronized (this) {
                refreshInProgress = false;
            }
        }
    }

    private boolean referenceDataIsStale(Instant now) {
        var marker = cacheEntryRepository.findById(REFERENCE_DATA_CACHE_KEY);
        return marker.isEmpty() || !marker.get().getExpiresAt().isAfter(now);
    }

    @Transactional
    public void refreshReferenceDataIfStale(String bearerToken) {
        if (!cacheEnabled() || normalized(bearerToken) == null) {
            return;
        }
        var now = Instant.now();
        var marker = cacheEntryRepository.findById(REFERENCE_DATA_CACHE_KEY);
        if (marker.isPresent() && marker.get().getExpiresAt().isAfter(now)) {
            return;
        }
        synchronized (this) {
            if (refreshFailureCooldownUntil != null && now.isBefore(refreshFailureCooldownUntil)) {
                return;
            }
        }
        try {
            refreshReferenceData(bearerToken.trim(), now);
            synchronized (this) {
                refreshFailureCooldownUntil = null;
            }
        } catch (RuntimeException exception) {
            synchronized (this) {
                refreshFailureCooldownUntil = now.plus(failureCooldown());
            }
            throw exception;
        }
    }

    @Transactional
    public void refreshReferenceData(String bearerToken, Instant now) {
        var expiresAt = now.plus(refreshInterval());
        var pageSize = pageSize();
        var maxPages = maxPages();

        var schools = items(digitalUniversityBridgeClient.getSchools(null, bearerToken));
        schools.forEach(userProvisioningService::upsertSchool);

        var programs = items(digitalUniversityBridgeClient.getEducationPrograms(null, bearerToken));
        programs.forEach(program -> upsertProgram(program, now));

        var employees = collectPaged(
                (page, size) -> digitalUniversityBridgeClient.getEmployees(null, page, size, bearerToken),
                pageSize,
                maxPages
        );
        employees.stream()
                .filter(employee -> longValue(employee.path("employeeId")) != null || longValue(employee.path("id")) != null)
                .forEach(employee -> userProvisioningService.upsertEmployee(employee, bearerToken, expiresAt, now));

        var teacherDisciplines = collectPaged(
                (page, size) -> digitalUniversityBridgeClient.getTeacherDisciplines(null, null, null, null, page, size, bearerToken),
                pageSize,
                maxPages
        );
        teacherDisciplines.forEach(item -> upsertCourseFromTeacherDiscipline(item, now));

        markReferenceDataSynced(now, expiresAt);
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

    private void upsertCourseFromTeacherDiscipline(JsonNode item, Instant now) {
        var subjectId = longValue(item.path("subjectId"));
        if (subjectId == null) {
            return;
        }

        var entity = courseRepository.findById("du-subject-" + subjectId)
                .orElseGet(CourseEntity::new);
        entity.setId("du-subject-" + subjectId);
        entity.setTitle(firstNonBlank(
                namedRef(item.path("subject")),
                namedRef(item.path("discipline")),
                normalized(item.path("subjectName").asText(null)),
                normalized(item.path("disciplineName").asText(null)),
                normalized(item.path("nameEn").asText(null)),
                normalized(item.path("nameRu").asText(null)),
                normalized(item.path("nameKk").asText(null)),
                "Subject " + subjectId
        ));
        entity.setCode(firstNonBlank(
                normalized(item.path("subjectCode").asText(null)),
                normalized(item.path("code").asText(null)),
                "DU-" + subjectId
        ));
        entity.setProgram(firstNonBlank(
                namedRef(item.path("program")),
                namedRef(item.path("educationProgram")),
                namedRef(item.path("degree")),
                "Digital University"
        ));
        var schoolId = longValue(item.path("school").path("id"));
        entity.setSchoolId(schoolId == null ? null : String.valueOf(schoolId));
        entity.setDegreeLevel(firstNonBlank(namedRef(item.path("degree")), "Program"));
        entity.setAcademicYear(academicYear(item, now));
        entity.setTrimester(termName(item.path("term")));
        entity.setLanguageOfInstruction(firstNonBlank(langName(item.path("languageName")), "English"));
        entity.setCredits(Math.max(1, integerValue(item.path("credits"), 1)));
        entity.setInstructorsCsv(instructorsCsv(item.path("teachers")));
        entity.setDisciplineTagsCsv(CourseMetadataSupport.toCsv(
                CourseMetadataSupport.defaultTags(entity.getTitle(), entity.getProgram(), entity.getCode())
        ));
        courseRepository.save(entity);
    }

    private List<JsonNode> collectPaged(PagedFetch fetchPage, int pageSize, int maxPages) {
        var result = new ArrayList<JsonNode>();
        var firstResponse = fetchPage.fetch(0, pageSize);
        var firstItems = items(firstResponse);
        result.addAll(firstItems);

        var totalItems = totalItems(firstResponse);
        if (totalItems != null && totalItems > result.size() && totalItems <= (long) pageSize * maxPages) {
            var allResponse = fetchPage.fetch(0, Math.toIntExact(totalItems));
            var allItems = items(allResponse);
            if (allItems.size() > result.size()) {
                return allItems;
            }
        }

        var firstTotalPages = totalPages(firstResponse, pageSize);
        if (firstTotalPages != null && firstTotalPages <= 1) {
            return result;
        }
        if (firstTotalPages == null && firstItems.size() < pageSize) {
            return result;
        }

        for (var page = 1; page < maxPages; page++) {
            var response = fetchPage.fetch(page, pageSize);
            var pageItems = items(response);
            result.addAll(pageItems);

            var totalPages = totalPages(response, pageSize);
            if (totalPages != null && page + 1 >= totalPages) {
                break;
            }
            if (totalPages == null && pageItems.size() < pageSize) {
                break;
            }
        }
        return result;
    }

    @FunctionalInterface
    private interface PagedFetch {
        JsonNode fetch(int page, int size);
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

    private Long totalItems(JsonNode response) {
        if (response == null || response.isMissingNode() || response.isNull()) {
            return null;
        }
        return firstLong(
                response.path("totalElements"),
                response.path("totalItems"),
                response.path("total"),
                response.path("count")
        );
    }

    private Integer totalPages(JsonNode response, int pageSize) {
        var explicit = integerValue(response.path("totalPages"));
        if (explicit != null) {
            return explicit;
        }
        var totalItems = totalItems(response);
        if (totalItems == null) {
            return null;
        }
        return (int) Math.ceil(totalItems / (double) Math.max(1, pageSize));
    }

    private void markReferenceDataSynced(Instant refreshedAt, Instant expiresAt) {
        var entity = cacheEntryRepository.findById(REFERENCE_DATA_CACHE_KEY)
                .orElseGet(DigitalUniversityCacheEntryEntity::new);
        entity.setId(REFERENCE_DATA_CACHE_KEY);
        entity.setPayloadJson("{}");
        entity.setRefreshedAt(refreshedAt);
        entity.setExpiresAt(expiresAt);
        cacheEntryRepository.save(entity);
    }

    private boolean cacheEnabled() {
        return properties.enabled() && properties.cache() != null && properties.cache().enabled();
    }

    private Duration refreshInterval() {
        var value = properties.cache() == null ? null : properties.cache().refreshInterval();
        return value == null ? Duration.ofHours(12) : value;
    }

    private Duration failureCooldown() {
        var value = properties.cache() == null ? null : properties.cache().failureCooldown();
        return value == null ? Duration.ofMinutes(15) : value;
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

    private Long firstLong(JsonNode... nodes) {
        for (var node : nodes) {
            var value = longValue(node);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private Integer integerValue(JsonNode node) {
        var value = longValue(node);
        if (value == null || value > Integer.MAX_VALUE || value < Integer.MIN_VALUE) {
            return null;
        }
        return value.intValue();
    }

    private int integerValue(JsonNode node, int fallback) {
        var value = integerValue(node);
        return value == null ? fallback : value;
    }

    private String academicYear(JsonNode item, Instant now) {
        var studyYears = item.path("studyYears");
        if (studyYears.isArray() && !studyYears.isEmpty()) {
            var first = normalized(studyYears.get(0).asText(null));
            if (first != null) {
                return first;
            }
        }
        var currentYear = java.time.LocalDate.ofInstant(now, java.time.ZoneId.systemDefault()).getYear();
        return currentYear + "-" + (currentYear + 1);
    }

    private String termName(JsonNode termNode) {
        var term = integerValue(termNode);
        return term == null ? "Term" : "Term " + term;
    }

    private String langName(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        return firstNonBlank(
                normalized(node.path("en").asText(null)),
                normalized(node.path("ru").asText(null)),
                normalized(node.path("kk").asText(null))
        );
    }

    private String instructorsCsv(JsonNode teachers) {
        if (!teachers.isArray()) {
            return "";
        }
        var names = new ArrayList<String>();
        for (var teacher : teachers) {
            names.add(firstNonBlank(
                    normalized(teacher.path("fullName").asText(null)),
                    String.join(" ", List.of(
                            Objects.toString(normalized(teacher.path("lastName").asText(null)), ""),
                            Objects.toString(normalized(teacher.path("firstName").asText(null)), ""),
                            Objects.toString(normalized(teacher.path("patronymic").asText(null)), "")
                    )).trim()
            ));
        }
        return String.join("|", names.stream()
                .map(this::normalized)
                .filter(Objects::nonNull)
                .toList());
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
