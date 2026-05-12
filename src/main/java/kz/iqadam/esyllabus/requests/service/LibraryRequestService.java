package kz.iqadam.esyllabus.requests.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import kz.iqadam.esyllabus.directory.model.StaffRole;
import kz.iqadam.esyllabus.directory.persistence.StaffProfileEntity;
import kz.iqadam.esyllabus.directory.service.DirectoryService;
import kz.iqadam.esyllabus.requests.api.LibraryRequestDecisionRequest;
import kz.iqadam.esyllabus.requests.api.LibraryRequestFeedbackRequest;
import kz.iqadam.esyllabus.requests.api.LibraryRequestResponse;
import kz.iqadam.esyllabus.requests.api.LibraryRequestUpsertRequest;
import kz.iqadam.esyllabus.requests.model.LibraryRequestStatus;
import kz.iqadam.esyllabus.requests.persistence.LibraryRequestEntity;
import kz.iqadam.esyllabus.requests.persistence.LibraryRequestItemEntity;
import kz.iqadam.esyllabus.requests.persistence.LibraryRequestItemRepository;
import kz.iqadam.esyllabus.requests.persistence.LibraryRequestRepository;
import kz.iqadam.esyllabus.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class LibraryRequestService {

    private static final List<LibraryRequestStatus> LIBRARIAN_VISIBLE_STATUSES = List.of(
            LibraryRequestStatus.APPROVED_BY_DIRECTOR,
            LibraryRequestStatus.FEEDBACK_PROVIDED
    );

    private final LibraryRequestRepository libraryRequestRepository;
    private final LibraryRequestItemRepository libraryRequestItemRepository;
    private final DirectoryService directoryService;
    private final LibraryRequestExportService libraryRequestExportService;

    public LibraryRequestService(
            LibraryRequestRepository libraryRequestRepository,
            LibraryRequestItemRepository libraryRequestItemRepository,
            DirectoryService directoryService,
            LibraryRequestExportService libraryRequestExportService
    ) {
        this.libraryRequestRepository = libraryRequestRepository;
        this.libraryRequestItemRepository = libraryRequestItemRepository;
        this.directoryService = directoryService;
        this.libraryRequestExportService = libraryRequestExportService;
    }

    public LibraryRequestResponse createRequest(CurrentUser user, LibraryRequestUpsertRequest request) {
        var staff = getRequesterProfile(user);
        var school = directoryService.getRequiredSchool(staff.getSchoolId());

        var entity = new LibraryRequestEntity();
        entity.setId("library-request-" + UUID.randomUUID());
        entity.setSyllabusId(null);
        entity.setRequesterUsername(user.email());
        entity.setRequesterName(staff.getFullName());
        entity.setSchoolId(school.getId());
        entity.setSchoolName(school.getName());
        entity.setDirectorUsername(school.getDirectorUsername());
        entity.setDepartment(defaulted(request.department(), school.getName()));
        entity.setEducationLevel(defaulted(request.educationLevel(), "Bachelor"));
        entity.setRequestDate(request.requestDate() == null ? LocalDate.now() : request.requestDate());
        entity.setStatus(LibraryRequestStatus.DRAFT);

        var saved = libraryRequestRepository.save(entity);
        replaceItems(saved.getId(), request.items());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<LibraryRequestResponse> getRequests(CurrentUser user, String status) {
        assertCanUseLibraryRequests(user);
        return resolveVisibleRequests(user).stream()
                .filter(item -> normalized(status) == null || item.getStatus().name().equalsIgnoreCase(status.trim()))
                .sorted(Comparator.comparing(LibraryRequestEntity::getUpdatedAt).reversed())
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public LibraryRequestResponse getRequest(CurrentUser user, String requestId) {
        assertCanUseLibraryRequests(user);
        var request = findRequest(requestId);
        assertCanRead(user, request);
        return toResponse(request);
    }

    public LibraryRequestResponse updateRequest(CurrentUser user, String requestId, LibraryRequestUpsertRequest request) {
        var entity = findRequest(requestId);
        assertCanEdit(user, entity);

        entity.setDepartment(defaulted(request.department(), entity.getSchoolName()));
        entity.setEducationLevel(defaulted(request.educationLevel(), entity.getEducationLevel()));
        entity.setRequestDate(request.requestDate() == null ? entity.getRequestDate() : request.requestDate());
        entity.setDirectorComment(null);
        entity.setLibraryFeedback(null);
        entity.setExpectedPurchaseMonth(null);
        var saved = libraryRequestRepository.save(entity);
        replaceItems(saved.getId(), request.items());
        return toResponse(saved);
    }

    public void deleteRequest(CurrentUser user, String requestId) {
        var entity = findRequest(requestId);
        assertCanEdit(user, entity);
        deleteRequestEntity(entity);
    }

    public LibraryRequestResponse submitForDirectorApproval(CurrentUser user, String requestId) {
        var entity = findRequest(requestId);
        assertCanEdit(user, entity);
        validateReadyToSubmit(entity);
        entity.setStatus(LibraryRequestStatus.PENDING_DIRECTOR_APPROVAL);
        entity.setDirectorComment(null);
        return toResponse(libraryRequestRepository.save(entity));
    }

    public LibraryRequestResponse approve(CurrentUser user, String requestId) {
        var entity = findRequest(requestId);
        assertIsDirectorForRequest(user, entity);
        if (entity.getStatus() != LibraryRequestStatus.PENDING_DIRECTOR_APPROVAL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only pending requests can be approved");
        }
        entity.setStatus(LibraryRequestStatus.APPROVED_BY_DIRECTOR);
        entity.setDirectorComment(null);
        return toResponse(libraryRequestRepository.save(entity));
    }

    public LibraryRequestResponse reject(CurrentUser user, String requestId, LibraryRequestDecisionRequest request) {
        var entity = findRequest(requestId);
        assertIsDirectorForRequest(user, entity);
        if (entity.getStatus() != LibraryRequestStatus.PENDING_DIRECTOR_APPROVAL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only pending requests can be rejected");
        }
        var comment = normalized(request == null ? null : request.comment());
        if (comment == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Director comment is required for rejection");
        }
        entity.setStatus(LibraryRequestStatus.REJECTED_BY_DIRECTOR);
        entity.setDirectorComment(comment);
        return toResponse(libraryRequestRepository.save(entity));
    }

    public LibraryRequestResponse leaveLibraryFeedback(CurrentUser user, String requestId, LibraryRequestFeedbackRequest request) {
        var entity = findRequest(requestId);
        assertIsLibrarian(user);
        if (!isVisibleToLibrarian(entity)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Library feedback can be added only after director approval");
        }
        var feedback = normalized(request == null ? null : request.feedback());
        if (feedback == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Library feedback is required");
        }
        var month = normalized(request.expectedPurchaseMonth());
        if (month != null) {
            try {
                YearMonth.parse(month);
            } catch (DateTimeParseException exception) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Expected purchase month must use yyyy-MM format");
            }
        }

        entity.setLibraryFeedback(feedback);
        entity.setExpectedPurchaseMonth(month);
        entity.setStatus(LibraryRequestStatus.FEEDBACK_PROVIDED);
        return toResponse(libraryRequestRepository.save(entity));
    }

    public String synchronizeApprovedSyllabusRequest(ApprovedSyllabusLibraryRequest request) {
        var existing = libraryRequestRepository.findBySyllabusId(request.syllabusId());
        var items = request.items() == null ? List.<ApprovedSyllabusItem>of() : request.items().stream()
                .filter(item -> normalized(item.title()) != null)
                .toList();

        if (items.isEmpty()) {
            existing.ifPresent(this::deleteRequestEntity);
            return null;
        }

        var requester = getTeachingRequesterProfile(request.requesterUsername());
        var schoolId = defaulted(request.schoolId(), requester.getSchoolId());
        var school = directoryService.getRequiredSchool(schoolId);
        var entity = existing.orElseGet(LibraryRequestEntity::new);

        if (entity.getId() == null) {
            entity.setId("library-request-" + UUID.randomUUID());
        }
        entity.setSyllabusId(request.syllabusId());
        entity.setRequesterUsername(request.requesterUsername());
        entity.setRequesterName(requester.getFullName());
        entity.setSchoolId(school.getId());
        entity.setSchoolName(school.getName());
        entity.setDirectorUsername(defaulted(request.directorUsername(), school.getDirectorUsername()));
        entity.setDepartment(defaulted(request.department(), school.getName()));
        entity.setEducationLevel(defaulted(request.educationLevel(), "Bachelor"));
        entity.setRequestDate(request.requestDate() == null ? LocalDate.now() : request.requestDate());
        entity.setDirectorComment(null);
        entity.setLibraryFeedback(null);
        entity.setExpectedPurchaseMonth(null);
        entity.setStatus(LibraryRequestStatus.APPROVED_BY_DIRECTOR);

        var saved = libraryRequestRepository.save(entity);
        replaceItemsFromApprovedSyllabus(saved.getId(), items);
        return saved.getId();
    }

    @Transactional(readOnly = true)
    public byte[] exportSingleForm(CurrentUser user, String requestId) {
        var request = getRequest(user, requestId);
        return libraryRequestExportService.exportSingleForm(request);
    }

    @Transactional(readOnly = true)
    public byte[] exportRequestsForLibrary(CurrentUser user) {
        assertIsLibrarian(user);
        return libraryRequestExportService.exportRequestsRegistry(
                findRequestsVisibleToLibrarian().stream()
                        .map(this::toResponse)
                        .toList()
        );
    }

    private void validateReadyToSubmit(LibraryRequestEntity entity) {
        var items = libraryRequestItemRepository.findByRequestIdOrderByLineNumberAsc(entity.getId());
        if (items.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one request item is required");
        }
        if (normalized(entity.getDepartment()) == null || normalized(entity.getEducationLevel()) == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Department and education level must be filled before submission");
        }
    }

    private List<LibraryRequestEntity> resolveVisibleRequests(CurrentUser user) {
        if (user.hasAnyRole("LIBRARIAN")) {
            return findRequestsVisibleToLibrarian();
        }
        if (user.hasAnyRole("DIRECTOR")) {
            var result = new LinkedHashMap<String, LibraryRequestEntity>();
            libraryRequestRepository.findByDirectorUsernameOrderByUpdatedAtDesc(user.email())
                    .forEach(item -> result.put(item.getId(), item));
            libraryRequestRepository.findByRequesterUsernameOrderByUpdatedAtDesc(user.email())
                    .forEach(item -> result.put(item.getId(), item));
            return List.copyOf(result.values());
        }
        return libraryRequestRepository.findByRequesterUsernameOrderByUpdatedAtDesc(user.email());
    }

    private List<LibraryRequestEntity> findRequestsVisibleToLibrarian() {
        return libraryRequestRepository.findByStatusInOrderByUpdatedAtDesc(LIBRARIAN_VISIBLE_STATUSES);
    }

    private void replaceItems(String requestId, List<LibraryRequestUpsertRequest.ItemRequest> items) {
        libraryRequestItemRepository.deleteByRequestId(requestId);
        var safeItems = items == null ? List.<LibraryRequestUpsertRequest.ItemRequest>of() : items;
        for (int index = 0; index < safeItems.size(); index++) {
            var item = safeItems.get(index);
            var entity = new LibraryRequestItemEntity();
            entity.setRequestId(requestId);
            entity.setLineNumber(index + 1);
            entity.setTitle(required(item.title(), "Book title is required"));
            entity.setAuthor(trimmed(item.author()));
            entity.setIsbn(trimmed(item.isbn()));
            entity.setPublisher(trimmed(item.publisher()));
            entity.setPublicationYear(trimmed(item.publicationYear()));
            entity.setDiscipline(required(item.discipline(), "Discipline is required"));
            entity.setEducationalProgram(required(item.educationalProgram(), "Educational program is required"));
            entity.setCourseNumber(item.courseNumber() == null ? 0 : Math.max(item.courseNumber(), 0));
            entity.setTrimester(required(item.trimester(), "Trimester is required"));
            entity.setQuantity(item.quantity() == null ? 1 : Math.max(item.quantity(), 1));
            entity.setLiteratureType(required(item.literatureType(), "Literature type is required"));
            libraryRequestItemRepository.save(entity);
        }
    }

    private void replaceItemsFromApprovedSyllabus(String requestId, List<ApprovedSyllabusItem> items) {
        libraryRequestItemRepository.deleteByRequestId(requestId);
        for (int index = 0; index < items.size(); index++) {
            var item = items.get(index);
            var entity = new LibraryRequestItemEntity();
            entity.setRequestId(requestId);
            entity.setLineNumber(index + 1);
            entity.setTitle(required(item.title(), "Book title is required"));
            entity.setAuthor(trimmed(item.author()));
            entity.setIsbn(trimmed(item.isbn()));
            entity.setPublisher(trimmed(item.publisher()));
            entity.setPublicationYear(trimmed(item.publicationYear()));
            entity.setDiscipline(required(item.discipline(), "Discipline is required"));
            entity.setEducationalProgram(required(item.educationalProgram(), "Educational program is required"));
            entity.setCourseNumber(item.courseNumber() == null ? 0 : Math.max(item.courseNumber(), 0));
            entity.setTrimester(required(item.trimester(), "Trimester is required"));
            entity.setQuantity(item.quantity() == null ? 1 : Math.max(item.quantity(), 1));
            entity.setLiteratureType(required(item.literatureType(), "Literature type is required"));
            libraryRequestItemRepository.save(entity);
        }
    }

    private LibraryRequestResponse toResponse(LibraryRequestEntity entity) {
        var items = libraryRequestItemRepository.findByRequestIdOrderByLineNumberAsc(entity.getId()).stream()
                .map(item -> new LibraryRequestResponse.ItemResponse(
                        item.getLineNumber(),
                        item.getTitle(),
                        item.getAuthor(),
                        item.getIsbn(),
                        item.getPublisher(),
                        item.getPublicationYear(),
                        item.getDiscipline(),
                        item.getEducationalProgram(),
                        item.getCourseNumber(),
                        item.getTrimester(),
                        item.getQuantity(),
                        item.getLiteratureType()
                ))
                .toList();

        return new LibraryRequestResponse(
                entity.getId(),
                entity.getRequesterUsername(),
                entity.getSyllabusId(),
                entity.getRequesterName(),
                entity.getSchoolId(),
                entity.getSchoolName(),
                entity.getDirectorUsername(),
                entity.getDepartment(),
                entity.getEducationLevel(),
                entity.getRequestDate(),
                entity.getStatus().frontendValue(),
                entity.getDirectorComment(),
                entity.getLibraryFeedback(),
                entity.getExpectedPurchaseMonth(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                items
        );
    }

    private LibraryRequestEntity findRequest(String requestId) {
        return libraryRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Library request not found"));
    }

    private StaffProfileEntity getRequesterProfile(CurrentUser user) {
        return getTeachingRequesterProfile(user.email());
    }

    private StaffProfileEntity getTeachingRequesterProfile(String username) {
        var staff = directoryService.getRequiredStaffProfile(username);
        if (!staff.getRole().isTeachingStaff()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only teaching staff can create library requests");
        }
        return staff;
    }

    private void assertCanUseLibraryRequests(CurrentUser user) {
        if (user.hasAnyRole("STUDENT")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Students cannot access library request workflow");
        }
    }

    private void assertCanRead(CurrentUser user, LibraryRequestEntity entity) {
        if (entity.getRequesterUsername().equalsIgnoreCase(user.email())
                || entity.getDirectorUsername().equalsIgnoreCase(user.email())) {
            return;
        }
        if (user.hasAnyRole("LIBRARIAN") && isVisibleToLibrarian(entity)) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to read this request");
    }

    private void assertCanEdit(CurrentUser user, LibraryRequestEntity entity) {
        if (!entity.getRequesterUsername().equalsIgnoreCase(user.email())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only requester can edit this request");
        }
        if (entity.getSyllabusId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Requests generated from approved syllabi cannot be edited manually");
        }
        if (entity.getStatus() != LibraryRequestStatus.DRAFT && entity.getStatus() != LibraryRequestStatus.REJECTED_BY_DIRECTOR) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only draft or rejected requests can be edited");
        }
    }

    private void assertIsDirectorForRequest(CurrentUser user, LibraryRequestEntity entity) {
        if (!entity.getDirectorUsername().equalsIgnoreCase(user.email())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only assigned school director can process this request");
        }
    }

    private void assertIsLibrarian(CurrentUser user) {
        if (user.hasAnyRole("LIBRARIAN")) {
            return;
        }
        var staff = directoryService.getRequiredStaffProfile(user.email());
        if (staff.getRole() != StaffRole.LIBRARIAN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only librarian can process library feedback");
        }
    }

    private boolean isVisibleToLibrarian(LibraryRequestEntity entity) {
        return LIBRARIAN_VISIBLE_STATUSES.contains(entity.getStatus());
    }

    private void deleteRequestEntity(LibraryRequestEntity entity) {
        libraryRequestItemRepository.deleteByRequestId(entity.getId());
        libraryRequestRepository.delete(entity);
    }

    private String required(String value, String message) {
        var normalized = normalized(value);
        if (normalized == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return normalized;
    }

    private String defaulted(String value, String fallback) {
        var normalized = normalized(value);
        return normalized == null ? fallback : normalized;
    }

    private String trimmed(String value) {
        return normalized(value);
    }

    private String normalized(String value) {
        if (value == null) {
            return null;
        }
        var result = value.trim();
        return result.isBlank() ? null : result;
    }

    public record ApprovedSyllabusLibraryRequest(
            String syllabusId,
            String requesterUsername,
            String directorUsername,
            String schoolId,
            String department,
            String educationLevel,
            LocalDate requestDate,
            List<ApprovedSyllabusItem> items
    ) {
    }

    public record ApprovedSyllabusItem(
            String title,
            String author,
            String isbn,
            String publisher,
            String publicationYear,
            String discipline,
            String educationalProgram,
            Integer courseNumber,
            String trimester,
            Integer quantity,
            String literatureType
    ) {
    }
}
