package kz.iqadam.esyllabus.syllabus.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import kz.iqadam.esyllabus.security.CurrentUser;
import kz.iqadam.esyllabus.syllabus.api.CourseCatalogItemResponse;
import kz.iqadam.esyllabus.syllabus.api.ImportLibraryResourcesRequest;
import kz.iqadam.esyllabus.syllabus.api.MySyllabusCardResponse;
import kz.iqadam.esyllabus.syllabus.api.SyllabusCreateRequest;
import kz.iqadam.esyllabus.syllabus.api.SyllabusResponse;
import kz.iqadam.esyllabus.syllabus.model.SyllabusStatus;
import kz.iqadam.esyllabus.syllabus.persistence.CourseEntity;
import kz.iqadam.esyllabus.syllabus.persistence.CourseRepository;
import kz.iqadam.esyllabus.syllabus.persistence.SyllabusEntity;
import kz.iqadam.esyllabus.syllabus.persistence.SyllabusRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class SyllabusService {

    private static final DateTimeFormatter CARD_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    private final CourseRepository courseRepository;
    private final SyllabusRepository syllabusRepository;
    private final ObjectMapper objectMapper;
    private final SyllabusContentFactory contentFactory;
    private final SyllabusMetricsCalculator metricsCalculator;

    public SyllabusService(
            CourseRepository courseRepository,
            SyllabusRepository syllabusRepository,
            ObjectMapper objectMapper,
            SyllabusContentFactory contentFactory,
            SyllabusMetricsCalculator metricsCalculator
    ) {
        this.courseRepository = courseRepository;
        this.syllabusRepository = syllabusRepository;
        this.objectMapper = objectMapper;
        this.contentFactory = contentFactory;
        this.metricsCalculator = metricsCalculator;
    }

    @Transactional(readOnly = true)
    public List<CourseCatalogItemResponse> getCourses(
            CurrentUser user,
            String search,
            String degree,
            String language,
            String status
    ) {
        var latestStatusesByCourseId = syllabusRepository.findByOwnerEmailAndCourseIdNotNullOrderByUpdatedAtDesc(user.email()).stream()
                .filter(item -> item.getCourseId() != null && !item.getCourseId().isBlank())
                .collect(Collectors.toMap(
                        SyllabusEntity::getCourseId,
                        SyllabusEntity::getStatus,
                        (existing, ignored) -> existing
                ));

        return courseRepository.findAll().stream()
                .map(course -> new CourseCatalogItemResponse(
                        course.getId(),
                        course.getTitle(),
                        course.getCode(),
                        course.getProgram(),
                        course.getSchoolId(),
                        course.getDegreeLevel(),
                        course.getAcademicYear(),
                        course.getTrimester(),
                        course.getLanguageOfInstruction(),
                        course.getCredits(),
                        latestStatusesByCourseId.getOrDefault(course.getId(), SyllabusStatus.DRAFT).frontendValue(),
                        SyllabusContentFactory.parseInstructors(course.getInstructorsCsv()),
                        CourseMetadataSupport.parseCsv(course.getDisciplineTagsCsv())
                ))
                .filter(item -> filterCourse(item, search, degree, language, status))
                .sorted(Comparator.comparing(CourseCatalogItemResponse::title))
                .toList();
    }

    @Transactional(readOnly = true)
    public CourseCatalogItemResponse getCourseById(CurrentUser user, String courseId) {
        var course = findCourse(courseId);
        var status = syllabusRepository.findTopByOwnerEmailAndCourseIdOrderByUpdatedAtDesc(user.email(), courseId)
                .map(SyllabusEntity::getStatus)
                .orElse(SyllabusStatus.DRAFT);
        return new CourseCatalogItemResponse(
                course.getId(),
                course.getTitle(),
                course.getCode(),
                course.getProgram(),
                course.getSchoolId(),
                course.getDegreeLevel(),
                course.getAcademicYear(),
                course.getTrimester(),
                course.getLanguageOfInstruction(),
                course.getCredits(),
                status.frontendValue(),
                SyllabusContentFactory.parseInstructors(course.getInstructorsCsv()),
                CourseMetadataSupport.parseCsv(course.getDisciplineTagsCsv())
        );
    }

    @Transactional(readOnly = true)
    public List<MySyllabusCardResponse> getMySyllabi(CurrentUser user) {
        return syllabusRepository.findByOwnerEmailOrderByUpdatedAtDesc(user.email()).stream()
                .map(item -> new MySyllabusCardResponse(
                        item.getId(),
                        item.getCourseId(),
                        item.getTitle(),
                        item.getCode(),
                        item.getProgram(),
                        CARD_DATE_TIME.format(item.getUpdatedAt()),
                        item.getStatus().frontendValue(),
                        item.getProgress(),
                        item.getSectionsCompleted(),
                        item.getSectionsTotal()
                ))
                .toList();
    }

    public SyllabusResponse createSyllabus(CurrentUser user, SyllabusCreateRequest request) {
        var courseId = normalized(request.courseId());
        if (courseId != null) {
            var existing = syllabusRepository.findTopByOwnerEmailAndCourseIdOrderByUpdatedAtDesc(user.email(), courseId);
            if (existing.isPresent() && existing.get().getStatus() != SyllabusStatus.PUBLISHED) {
                return toResponse(existing.get());
            }
        }
        var content = courseId == null
                ? contentFactory.createBlank()
                : contentFactory.createFromCourse(findCourse(courseId));

        var syllabus = new SyllabusEntity();
        syllabus.setId("syllabus-" + UUID.randomUUID());
        syllabus.setCourseId(courseId);
        syllabus.setOwnerEmail(user.email());
        syllabus.setStatus(SyllabusStatus.DRAFT);
        syllabus.setReviewComment(null);
        syncSyllabusFromContent(syllabus, content);
        return toResponse(syllabusRepository.save(syllabus));
    }

    @Transactional(readOnly = true)
    public SyllabusResponse getSyllabus(CurrentUser user, String syllabusId) {
        var syllabus = findSyllabus(syllabusId);
        assertCanRead(user, syllabus);
        return toResponse(syllabus);
    }

    public SyllabusResponse updateSyllabus(CurrentUser user, String syllabusId, JsonNode content) {
        var syllabus = findSyllabus(syllabusId);
        assertCanEdit(user, syllabus);

        syncSyllabusFromContent(syllabus, content);
        syllabus.setReviewComment(null);
        return toResponse(syllabusRepository.save(syllabus));
    }

    public SyllabusResponse submitForReview(CurrentUser user, String syllabusId) {
        var syllabus = findSyllabus(syllabusId);
        assertCanEdit(user, syllabus);

        var metrics = metricsCalculator.calculate(readContent(syllabus));
        if (!metrics.readyForReview()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Syllabus is not complete yet. Fill all sections before sending to review."
            );
        }

        syllabus.setStatus(SyllabusStatus.NEEDS_REVIEW);
        syllabus.setReviewComment(null);
        return toResponse(syllabusRepository.save(syllabus));
    }

    public SyllabusResponse approve(CurrentUser user, String syllabusId) {
        var syllabus = findSyllabus(syllabusId);
        if (!user.hasAnyRole("DIRECTOR", "PROFESSOR")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only director or professor can approve syllabi");
        }
        if (syllabus.getStatus() != SyllabusStatus.NEEDS_REVIEW) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only syllabi in review can be approved");
        }

        syllabus.setStatus(SyllabusStatus.PUBLISHED);
        syllabus.setReviewComment(null);
        return toResponse(syllabusRepository.save(syllabus));
    }

    public SyllabusResponse returnForFix(CurrentUser user, String syllabusId, String comment) {
        var syllabus = findSyllabus(syllabusId);
        if (!user.hasAnyRole("DIRECTOR", "PROFESSOR")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only director or professor can return syllabus for fixes");
        }
        if (syllabus.getStatus() != SyllabusStatus.NEEDS_REVIEW) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only syllabi in review can be returned for fixes");
        }
        var normalizedComment = Objects.toString(comment, "").trim();
        if (normalizedComment.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comment is required when returning syllabus for fixes");
        }

        syllabus.setStatus(SyllabusStatus.DRAFT);
        syllabus.setReviewComment(normalizedComment);
        return toResponse(syllabusRepository.save(syllabus));
    }

    public SyllabusResponse importLibraryResources(
            CurrentUser user,
            String syllabusId,
            ImportLibraryResourcesRequest request
    ) {
        var syllabus = findSyllabus(syllabusId);
        assertCanEdit(user, syllabus);

        var resources = request.books() == null ? List.<ImportLibraryResourcesRequest.LibraryBookItem>of() : request.books();
        if (resources.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No books provided for import");
        }

        var content = readContent(syllabus).deepCopy();
        if (!content.isObject()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Syllabus content must be a JSON object");
        }
        var root = (com.fasterxml.jackson.databind.node.ObjectNode) content;
        var resourcesArray = root.withArray("resources");
        contentFactory.appendResources(resourcesArray, resources.stream()
                .map(book -> new SyllabusContentFactory.LibraryResourceSeed(
                        safe(book.title()),
                        safe(book.author()),
                        safe(book.year()),
                        safe(book.type()),
                        safe(book.url())
                ))
                .toList());

        syncSyllabusFromContent(syllabus, root);
        return toResponse(syllabusRepository.save(syllabus));
    }

    private void syncSyllabusFromContent(SyllabusEntity syllabus, JsonNode content) {
        var normalized = normalizeContent(content);
        var metrics = metricsCalculator.calculate(normalized);
        syllabus.setTitle(textOrFallback(normalized, "title", syllabus.getTitle(), "Untitled syllabus"));
        syllabus.setCode(textOrFallback(normalized, "code", syllabus.getCode(), "UNSET"));
        syllabus.setProgram(textOrFallback(normalized, "program", syllabus.getProgram(), "Program is not set"));
        syllabus.setContentJson(writeContent(normalized));
        syllabus.setProgress(metrics.progress());
        syllabus.setSectionsCompleted(metrics.sectionsCompleted());
        syllabus.setSectionsTotal(metrics.sectionsTotal());
    }

    private boolean filterCourse(
            CourseCatalogItemResponse item,
            String search,
            String degree,
            String language,
            String status
    ) {
        var matchesSearch = normalized(search) == null
                || (item.title() + " " + item.code() + " " + item.program() + " " + String.join(" ", item.instructors())).toLowerCase()
                .contains(search.trim().toLowerCase());
        var matchesDegree = normalized(degree) == null || item.degreeLevel().equalsIgnoreCase(degree.trim());
        var matchesLanguage = normalized(language) == null || item.languageOfInstruction().equalsIgnoreCase(language.trim());
        var matchesStatus = normalized(status) == null || item.status().equalsIgnoreCase(status.trim());
        return matchesSearch && matchesDegree && matchesLanguage && matchesStatus;
    }

    private SyllabusEntity findSyllabus(String id) {
        return syllabusRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Syllabus not found"));
    }

    private CourseEntity findCourse(String id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
    }

    private JsonNode readContent(SyllabusEntity syllabus) {
        try {
            return objectMapper.readTree(syllabus.getContentJson());
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Stored syllabus content is corrupted");
        }
    }

    private String writeContent(JsonNode content) {
        try {
            return objectMapper.writeValueAsString(content);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to serialize syllabus content");
        }
    }

    private String textOrFallback(JsonNode content, String field, String current, String fallback) {
        var value = content.path(field).asText("").trim();
        if (!value.isBlank()) {
            return value;
        }
        if (current != null && !current.isBlank()) {
            return current;
        }
        return fallback;
    }

    private void assertCanRead(CurrentUser user, SyllabusEntity syllabus) {
        if (syllabus.getOwnerEmail().equalsIgnoreCase(user.email())) {
            return;
        }
        if (user.hasAnyRole("DIRECTOR", "PROFESSOR")) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to read this syllabus");
    }

    private void assertCanEdit(CurrentUser user, SyllabusEntity syllabus) {
        if (!syllabus.getOwnerEmail().equalsIgnoreCase(user.email())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only owner can edit syllabus");
        }
        if (syllabus.getStatus() != SyllabusStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only draft syllabi can be edited");
        }
    }

    private JsonNode normalizeContent(JsonNode content) {
        if (content == null || !content.isObject()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Syllabus content must be a JSON object");
        }

        var root = (com.fasterxml.jackson.databind.node.ObjectNode) content.deepCopy();
        var workload = root.with("workload");
        var lectures = Math.max(0, workload.path("lecturesHours").asInt(0));
        var practice = Math.max(0, workload.path("practiceHours").asInt(0));
        var lab = Math.max(0, workload.path("labHours").asInt(0));
        var iass = Math.max(0, workload.path("iassHours").asInt(0));
        var sis = Math.max(0, workload.path("sisHours").asInt(0));

        workload.put("lecturesHours", lectures);
        workload.put("practiceHours", practice);
        workload.put("labHours", lab);
        workload.put("iassHours", iass);
        workload.put("sisHours", sis);
        workload.put("totalHours", lectures + practice + lab + iass + sis);
        return root;
    }

    private SyllabusResponse toResponse(SyllabusEntity entity) {
        return new SyllabusResponse(
                entity.getId(),
                entity.getCourseId(),
                entity.getOwnerEmail(),
                entity.getStatus().frontendValue(),
                entity.getProgress(),
                entity.getSectionsCompleted(),
                entity.getSectionsTotal(),
                entity.getReviewComment(),
                entity.getUpdatedAt(),
                readContent(entity)
        );
    }

    private String normalized(String value) {
        if (value == null) {
            return null;
        }
        var result = value.trim();
        return result.isBlank() ? null : result;
    }

    private String safe(String value) {
        return Objects.toString(value, "").trim();
    }
}
