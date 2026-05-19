package kz.iqadam.esyllabus.syllabus.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import kz.iqadam.esyllabus.directory.model.StaffRole;
import kz.iqadam.esyllabus.directory.persistence.StaffProfileEntity;
import kz.iqadam.esyllabus.directory.service.DirectoryService;
import kz.iqadam.esyllabus.requests.service.LibraryRequestService;
import kz.iqadam.esyllabus.security.CurrentUser;
import kz.iqadam.esyllabus.syllabus.api.CourseCatalogItemResponse;
import kz.iqadam.esyllabus.syllabus.api.ImportLibraryResourcesRequest;
import kz.iqadam.esyllabus.syllabus.api.MySyllabusCardResponse;
import kz.iqadam.esyllabus.syllabus.api.SyllabusCreateRequest;
import kz.iqadam.esyllabus.syllabus.api.SyllabusDirectorUpdateRequest;
import kz.iqadam.esyllabus.syllabus.api.SyllabusMetadataOptionsResponse;
import kz.iqadam.esyllabus.syllabus.api.SyllabusResponse;
import kz.iqadam.esyllabus.syllabus.api.SyllabusReviewQueueItemResponse;
import kz.iqadam.esyllabus.syllabus.api.SyllabusReviewerResponse;
import kz.iqadam.esyllabus.syllabus.api.SyllabusReviewersUpdateRequest;
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
    private final DirectoryService directoryService;
    private final LibraryRequestService libraryRequestService;

    public SyllabusService(
            CourseRepository courseRepository,
            SyllabusRepository syllabusRepository,
            ObjectMapper objectMapper,
            SyllabusContentFactory contentFactory,
            SyllabusMetricsCalculator metricsCalculator,
            DirectoryService directoryService,
            LibraryRequestService libraryRequestService
    ) {
        this.courseRepository = courseRepository;
        this.syllabusRepository = syllabusRepository;
        this.objectMapper = objectMapper;
        this.contentFactory = contentFactory;
        this.metricsCalculator = metricsCalculator;
        this.directoryService = directoryService;
        this.libraryRequestService = libraryRequestService;
    }

    @Transactional(readOnly = true)
    public List<CourseCatalogItemResponse> getCourses(
            CurrentUser user,
            String search,
            String degree,
            String language,
            String status
    ) {
        return getCoursesForStaff(user, search, degree, language, status);
    }

    @Transactional(readOnly = true)
    public CourseCatalogItemResponse getCourseById(CurrentUser user, String courseId) {
        var course = findCourse(courseId);
        var latest = syllabusRepository.findTopByOwnerEmailAndCourseIdOrderByUpdatedAtDesc(user.email(), courseId).orElse(null);
        return toCourseCatalogItem(course, latest == null ? SyllabusStatus.DRAFT : latest.getStatus(), latest == null ? null : latest.getId());
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

    @Transactional(readOnly = true)
    public List<SyllabusReviewQueueItemResponse> getReviewQueue(CurrentUser user) {
        var result = new LinkedHashMap<String, SyllabusEntity>();

        if (user.hasAnyRole("DIRECTOR")) {
            syllabusRepository.findByDirectorUsernameOrderByUpdatedAtDesc(user.email()).stream()
                    .filter(item -> item.getStatus().isPendingDirectorReview())
                    .forEach(item -> result.put(item.getId(), item));
        }

        if (!user.hasAnyRole("LIBRARIAN") && !user.hasAnyRole("DIRECTOR")) {
            syllabusRepository.findByStatusOrderByUpdatedAtDesc(SyllabusStatus.PENDING_COLLEAGUE_CONFIRMATION).stream()
                    .filter(item -> reviewerUsernames(item).contains(user.email()))
                    .forEach(item -> result.put(item.getId(), item));
        }

        return result.values().stream()
                .sorted(Comparator.comparing(SyllabusEntity::getUpdatedAt).reversed())
                .map(this::toReviewQueueItem)
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
        syllabus.setDirectorUsername(resolveDirectorUsername(user, courseId));
        syllabus.setStatus(SyllabusStatus.DRAFT);
        syllabus.setReviewComment(null);
        syllabus.setReviewerUsernamesCsv("");
        syllabus.setApprovedReviewerUsernamesCsv("");
        syllabus.setLinkedLibraryRequestId(null);
        syncSyllabusFromContent(syllabus, content);
        return toResponse(syllabusRepository.save(syllabus));
    }

    public SyllabusResponse updateReviewers(CurrentUser user, String syllabusId, SyllabusReviewersUpdateRequest request) {
        var syllabus = findSyllabus(syllabusId);
        assertCanEdit(user, syllabus);

        var reviewers = resolveReviewerProfiles(user, request == null ? null : request.reviewerUsernames());
        syllabus.setReviewerUsernamesCsv(CourseMetadataSupport.toCsv(reviewers.stream()
                .map(StaffProfileEntity::getUsername)
                .toList()));
        syllabus.setApprovedReviewerUsernamesCsv("");
        syllabus.setReviewComment(null);
        return toResponse(syllabusRepository.save(syllabus));
    }

    public SyllabusResponse updateDirector(CurrentUser user, String syllabusId, SyllabusDirectorUpdateRequest request) {
        var syllabus = findSyllabus(syllabusId);
        assertCanEdit(user, syllabus);

        syllabus.setDirectorUsername(resolveAssignedDirectorUsername(user, request == null ? null : request.directorUsername()));
        syllabus.setReviewComment(null);
        return toResponse(syllabusRepository.save(syllabus));
    }

    @Transactional(readOnly = true)
    public SyllabusMetadataOptionsResponse getMetadataOptions(CurrentUser user, String syllabusId) {
        var syllabus = findSyllabus(syllabusId);
        assertCanRead(user, syllabus);

        var owner = directoryService.getRequiredStaffProfile(syllabus.getOwnerEmail());
        var schoolId = owner.getSchoolId();

        return new SyllabusMetadataOptionsResponse(
                directoryService.getAllowedInstructors(schoolId),
                directoryService.getAllowedReviewers(schoolId, syllabusId),
                directoryService.getAllowedDirectors(schoolId),
                directoryService.getSchools(),
                directoryService.getPrograms(schoolId, null, null),
                directoryService.getAcademicYears(),
                directoryService.getDegreeLevels(),
                directoryService.getCourseTypes(),
                directoryService.getAssessmentStages(),
                directoryService.getTrimesters(),
                directoryService.getLanguages()
        );
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
            throw new SyllabusReviewValidationException(
                    "Syllabus is not complete yet. Fill all required sections before sending to review.",
                    metrics
            );
        }

        syllabus.setDirectorUsername(resolveAssignedDirectorUsername(user, syllabus.getDirectorUsername()));
        if (normalized(syllabus.getDirectorUsername()) == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "School director must be selected before sending syllabus to review");
        }
        syllabus.setApprovedReviewerUsernamesCsv("");
        syllabus.setReviewComment(null);
        syllabus.setStatus(reviewerUsernames(syllabus).isEmpty()
                ? SyllabusStatus.PENDING_DIRECTOR_REVIEW
                : SyllabusStatus.PENDING_COLLEAGUE_CONFIRMATION);
        return toResponse(syllabusRepository.save(syllabus));
    }

    public SyllabusResponse approveColleague(CurrentUser user, String syllabusId) {
        var syllabus = findSyllabus(syllabusId);
        if (syllabus.getStatus() != SyllabusStatus.PENDING_COLLEAGUE_CONFIRMATION) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only syllabi pending colleague confirmation can be approved");
        }
        if (isDirectorForSyllabus(user, syllabus)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Director approval is the final step and becomes available only after all colleague confirmations"
            );
        }
        if (!reviewerUsernames(syllabus).contains(user.email())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only assigned colleagues can approve this syllabus");
        }

        var approved = new LinkedHashSet<>(approvedReviewerUsernames(syllabus));
        approved.add(user.email());
        syllabus.setApprovedReviewerUsernamesCsv(CourseMetadataSupport.toCsv(List.copyOf(approved)));
        syllabus.setReviewComment(null);
        if (approved.containsAll(reviewerUsernames(syllabus))) {
            syllabus.setStatus(SyllabusStatus.PENDING_DIRECTOR_REVIEW);
        }
        return toResponse(syllabusRepository.save(syllabus));
    }

    public SyllabusResponse approve(CurrentUser user, String syllabusId) {
        var syllabus = findSyllabus(syllabusId);
        if (!isDirectorForSyllabus(user, syllabus)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only assigned school director can approve syllabi");
        }
        if (!syllabus.getStatus().isPendingDirectorReview()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only syllabi in director review can be approved");
        }

        syllabus.setStatus(SyllabusStatus.PUBLISHED);
        syllabus.setReviewComment(null);
        syllabus.setLinkedLibraryRequestId(synchronizeApprovedSyllabusLibraryRequest(syllabus));
        return toResponse(syllabusRepository.save(syllabus));
    }

    public SyllabusResponse returnForFix(CurrentUser user, String syllabusId, String comment) {
        var syllabus = findSyllabus(syllabusId);
        var normalizedComment = Objects.toString(comment, "").trim();
        if (normalizedComment.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comment is required when returning syllabus for fixes");
        }

        if (syllabus.getStatus() == SyllabusStatus.PENDING_COLLEAGUE_CONFIRMATION && reviewerUsernames(syllabus).contains(user.email())) {
            syllabus.setStatus(SyllabusStatus.DRAFT);
            syllabus.setApprovedReviewerUsernamesCsv("");
            syllabus.setReviewComment(normalizedComment);
            return toResponse(syllabusRepository.save(syllabus));
        }
        if (syllabus.getStatus().isPendingDirectorReview() && isDirectorForSyllabus(user, syllabus)) {
            syllabus.setStatus(SyllabusStatus.DRAFT);
            syllabus.setApprovedReviewerUsernamesCsv("");
            syllabus.setReviewComment(normalizedComment);
            return toResponse(syllabusRepository.save(syllabus));
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Current user cannot return this syllabus for fixes");
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
        var root = (ObjectNode) content;
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

    private List<CourseCatalogItemResponse> getCoursesForStaff(
            CurrentUser user,
            String search,
            String degree,
            String language,
            String status
    ) {
        var latestByCourseId = syllabusRepository.findByOwnerEmailAndCourseIdNotNullOrderByUpdatedAtDesc(user.email()).stream()
                .filter(item -> normalized(item.getCourseId()) != null)
                .collect(Collectors.toMap(
                        SyllabusEntity::getCourseId,
                        item -> item,
                        (existing, ignored) -> existing
                ));

        return courseRepository.findAll().stream()
                .map(course -> {
                    var latest = latestByCourseId.get(course.getId());
                    return toCourseCatalogItem(course, latest == null ? SyllabusStatus.DRAFT : latest.getStatus(), latest == null ? null : latest.getId());
                })
                .filter(item -> filterCourse(item, search, degree, language, status))
                .sorted(Comparator.comparing(CourseCatalogItemResponse::title))
                .toList();
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
                || (item.title() + " " + item.code() + " " + item.program() + " " + String.join(" ", item.instructors())).toLowerCase(Locale.ROOT)
                .contains(search.trim().toLowerCase(Locale.ROOT));
        var matchesDegree = normalized(degree) == null || item.degreeLevel().equalsIgnoreCase(degree.trim());
        var matchesLanguage = normalized(language) == null || item.languageOfInstruction().equalsIgnoreCase(language.trim());
        var matchesStatus = normalized(status) == null || item.status().equalsIgnoreCase(status.trim());
        return matchesSearch && matchesDegree && matchesLanguage && matchesStatus;
    }

    private String synchronizeApprovedSyllabusLibraryRequest(SyllabusEntity syllabus) {
        var owner = directoryService.getRequiredStaffProfile(syllabus.getOwnerEmail());
        var course = syllabus.getCourseId() == null ? null : findCourse(syllabus.getCourseId());
        var content = readContent(syllabus);
        var items = buildApprovedSyllabusItems(syllabus, course, content);

        return libraryRequestService.synchronizeApprovedSyllabusRequest(
                new LibraryRequestService.ApprovedSyllabusLibraryRequest(
                        syllabus.getId(),
                        syllabus.getOwnerEmail(),
                        syllabus.getDirectorUsername(),
                        course == null ? owner.getSchoolId() : defaulted(course.getSchoolId(), owner.getSchoolId()),
                        defaulted(course == null ? null : course.getProgram(), syllabus.getProgram()),
                        defaulted(content.path("degreeLevel").asText(""), course == null ? null : course.getDegreeLevel()),
                        LocalDate.now(),
                        items
                )
        );
    }

    private List<LibraryRequestService.ApprovedSyllabusItem> buildApprovedSyllabusItems(
            SyllabusEntity syllabus,
            CourseEntity course,
            JsonNode content
    ) {
        var trimester = defaulted(content.path("trimester").asText(""), course == null ? null : course.getTrimester());
        var discipline = defaulted(syllabus.getTitle(), course == null ? null : course.getTitle());
        var program = defaulted(syllabus.getProgram(), course == null ? null : course.getProgram());
        var courseNumber = inferCourseNumber(syllabus.getCode());

        if (!content.path("resources").isArray()) {
            return List.of();
        }

        var result = new java.util.ArrayList<LibraryRequestService.ApprovedSyllabusItem>();
        for (var resource : content.path("resources")) {
            var title = normalized(resource.path("title").asText(""));
            if (title == null) {
                continue;
            }
            result.add(new LibraryRequestService.ApprovedSyllabusItem(
                    title,
                    normalized(resource.path("author").asText("")),
                    normalized(resource.path("isbn").asText("")),
                    normalized(resource.path("publisher").asText("")),
                    normalized(resource.path("year").asText("")),
                    defaulted(resource.path("discipline").asText(""), discipline),
                    program,
                    courseNumber,
                    defaulted(trimester, "Unknown"),
                    resource.path("quantity").isInt() ? Math.max(resource.path("quantity").asInt(1), 1) : 1,
                    normalizeLiteratureType(resource)
            ));
        }
        return result;
    }

    private Integer inferCourseNumber(String code) {
        if (code == null) {
            return 0;
        }
        var digits = code.replaceAll("[^0-9]", "");
        if (digits.isBlank()) {
            return 0;
        }
        return Math.max(Character.getNumericValue(digits.charAt(0)), 0);
    }

    private String normalizeLiteratureType(JsonNode resource) {
        var type = normalized(resource.path("type").asText(""));
        if (type != null) {
            return type;
        }
        return resource.path("isRequired").asBoolean(false) ? "Required literature" : "Additional literature";
    }

    private CourseCatalogItemResponse toCourseCatalogItem(CourseEntity course, SyllabusStatus syllabusStatus, String syllabusId) {
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
                syllabusStatus.frontendValue(),
                SyllabusContentFactory.parseInstructors(course.getInstructorsCsv()),
                CourseMetadataSupport.parseCsv(course.getDisciplineTagsCsv()),
                syllabusId
        );
    }

    private SyllabusReviewQueueItemResponse toReviewQueueItem(SyllabusEntity entity) {
        return new SyllabusReviewQueueItemResponse(
                entity.getId(),
                entity.getCourseId(),
                entity.getTitle(),
                entity.getCode(),
                entity.getProgram(),
                entity.getOwnerEmail(),
                entity.getDirectorUsername(),
                CARD_DATE_TIME.format(entity.getUpdatedAt()),
                entity.getStatus().frontendValue(),
                reviewerResponses(entity)
        );
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
        if (isDirectorForSyllabus(user, syllabus)) {
            return;
        }
        if (reviewerUsernames(syllabus).contains(user.email())) {
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

    private boolean isDirectorForSyllabus(CurrentUser user, SyllabusEntity syllabus) {
        return user.hasAnyRole("DIRECTOR")
                && syllabus.getDirectorUsername() != null
                && syllabus.getDirectorUsername().equalsIgnoreCase(user.email());
    }

    private List<StaffProfileEntity> resolveReviewerProfiles(CurrentUser user, List<String> reviewerUsernames) {
        var owner = directoryService.getRequiredStaffProfile(user.email());
        var usernames = reviewerUsernames == null ? List.<String>of() : reviewerUsernames.stream()
                .map(this::normalized)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (usernames.isEmpty()) {
            return List.of();
        }

        var reviewers = directoryService.getStaffByUsernames(usernames);
        if (reviewers.size() != usernames.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Some colleagues were not found in the staff directory");
        }
        if (reviewers.stream().anyMatch(item -> item.getUsername().equalsIgnoreCase(user.email()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Syllabus owner cannot add themselves as a confirming colleague");
        }
        if (reviewers.stream().anyMatch(item -> item.getRole() == StaffRole.LIBRARIAN)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Librarians cannot be added as syllabus reviewers");
        }
        if (reviewers.stream().anyMatch(item -> item.getRole() == StaffRole.SCHOOL_DIRECTOR)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "School director cannot be added as a colleague reviewer because director approval happens last"
            );
        }
        if (reviewers.stream().anyMatch(item -> !Objects.equals(item.getSchoolId(), owner.getSchoolId()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only colleagues from the same school can be assigned for confirmation");
        }
        return reviewers;
    }

    private String resolveDirectorUsername(CurrentUser user, String courseId) {
        if (courseId != null) {
            var course = findCourse(courseId);
            var schoolId = normalized(course.getSchoolId());
            if (schoolId != null) {
                return directoryService.getRequiredSchool(schoolId).getDirectorUsername();
            }
        }
        var owner = directoryService.getRequiredStaffProfile(user.email());
        return directoryService.getRequiredSchool(owner.getSchoolId()).getDirectorUsername();
    }

    private String resolveAssignedDirectorUsername(CurrentUser user, String directorUsername) {
        var owner = directoryService.getRequiredStaffProfile(user.email());
        var resolvedDirectorUsername = normalized(directorUsername);
        if (resolvedDirectorUsername == null) {
            return resolveDirectorUsername(user, null);
        }

        var directorProfile = directoryService.getRequiredStaffProfile(resolvedDirectorUsername);
        if (directorProfile.getRole() != StaffRole.SCHOOL_DIRECTOR) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected director must have SCHOOL_DIRECTOR role");
        }
        if (!Objects.equals(directorProfile.getSchoolId(), owner.getSchoolId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected director must belong to the same school as the syllabus owner");
        }
        return directorProfile.getUsername();
    }

    private List<String> reviewerUsernames(SyllabusEntity entity) {
        return CourseMetadataSupport.parseCsv(entity.getReviewerUsernamesCsv());
    }

    private Set<String> approvedReviewerUsernames(SyllabusEntity entity) {
        return new LinkedHashSet<>(CourseMetadataSupport.parseCsv(entity.getApprovedReviewerUsernamesCsv()));
    }

    private List<SyllabusReviewerResponse> reviewerResponses(SyllabusEntity entity) {
        var reviewerUsernames = reviewerUsernames(entity);
        if (reviewerUsernames.isEmpty()) {
            return List.of();
        }

        var approved = approvedReviewerUsernames(entity);
        var byUsername = directoryService.getStaffByUsernames(reviewerUsernames).stream()
                .collect(Collectors.toMap(StaffProfileEntity::getUsername, item -> item, (left, right) -> left));

        return reviewerUsernames.stream()
                .map(username -> {
                    var profile = byUsername.get(username);
                    return new SyllabusReviewerResponse(
                            username,
                            profile == null ? username : profile.getFullName(),
                            profile == null ? "TEACHER" : profile.getRole().apiValue(),
                            approved.contains(username)
                    );
                })
                .toList();
    }

    private JsonNode normalizeContent(JsonNode content) {
        if (content == null || !content.isObject()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Syllabus content must be a JSON object");
        }

        var root = (ObjectNode) content.deepCopy();
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
                entity.getDirectorUsername(),
                entity.getStatus().frontendValue(),
                entity.getProgress(),
                entity.getSectionsCompleted(),
                entity.getSectionsTotal(),
                entity.getReviewComment(),
                reviewerResponses(entity),
                entity.getLinkedLibraryRequestId(),
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

    private String defaulted(String value, String fallback) {
        var normalized = normalized(value);
        return normalized == null ? fallback : normalized;
    }

    private String safe(String value) {
        return Objects.toString(value, "").trim();
    }
}
