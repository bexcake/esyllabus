package kz.iqadam.esyllabus.requests.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
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
        var currentStaff = directoryService.getRequiredStaffProfile(user.email());
        var requests = resolveVisibleRequests(user, currentStaff);
        return requests.stream()
                .filter(item -> normalized(status) == null || item.getStatus().name().equalsIgnoreCase(status.trim()))
                .sorted(Comparator.comparing(LibraryRequestEntity::getUpdatedAt).reversed())
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public LibraryRequestResponse getRequest(CurrentUser user, String requestId) {
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
        libraryRequestItemRepository.deleteByRequestId(entity.getId());
        libraryRequestRepository.delete(entity);
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
        if (entity.getStatus() != LibraryRequestStatus.APPROVED_BY_DIRECTOR
                && entity.getStatus() != LibraryRequestStatus.FEEDBACK_PROVIDED) {
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

    @Transactional(readOnly = true)
    public byte[] exportSingleForm(CurrentUser user, String requestId) {
        var request = getRequest(user, requestId);
        return libraryRequestExportService.exportSingleForm(request);
    }

    @Transactional(readOnly = true)
    public byte[] exportRequestsForLibrary(CurrentUser user) {
        assertIsLibrarian(user);
        return libraryRequestExportService.exportRequestsRegistry(
                libraryRequestRepository.findAll().stream()
                        .sorted(Comparator.comparing(LibraryRequestEntity::getUpdatedAt).reversed())
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Department and education level must be заполнены before submission");
        }
    }

    private List<LibraryRequestEntity> resolveVisibleRequests(CurrentUser user, StaffProfileEntity staff) {
        if (staff.getRole() == StaffRole.LIBRARIAN || user.hasAnyRole("LIBRARIAN")) {
            return libraryRequestRepository.findAll();
        }
        if (staff.getRole() == StaffRole.SCHOOL_DIRECTOR || user.hasAnyRole("DIRECTOR")) {
            var result = new LinkedHashMap<String, LibraryRequestEntity>();
            libraryRequestRepository.findByDirectorUsernameOrderByUpdatedAtDesc(user.email())
                    .forEach(item -> result.put(item.getId(), item));
            libraryRequestRepository.findByRequesterUsernameOrderByUpdatedAtDesc(user.email())
                    .forEach(item -> result.put(item.getId(), item));
            return List.copyOf(result.values());
        }
        return libraryRequestRepository.findByRequesterUsernameOrderByUpdatedAtDesc(user.email());
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
        var staff = directoryService.getRequiredStaffProfile(user.email());
        if (staff.getRole() == StaffRole.LIBRARIAN || staff.getRole() == StaffRole.SCHOOL_DIRECTOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only teaching staff can create library requests");
        }
        return staff;
    }

    private void assertCanRead(CurrentUser user, LibraryRequestEntity entity) {
        if (entity.getRequesterUsername().equalsIgnoreCase(user.email())
                || entity.getDirectorUsername().equalsIgnoreCase(user.email())
                || user.hasAnyRole("LIBRARIAN")) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to read this request");
    }

    private void assertCanEdit(CurrentUser user, LibraryRequestEntity entity) {
        if (!entity.getRequesterUsername().equalsIgnoreCase(user.email())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only requester can edit this request");
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
        var staff = directoryService.getRequiredStaffProfile(user.email());
        if (staff.getRole() != StaffRole.LIBRARIAN && !user.hasAnyRole("LIBRARIAN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only librarian can process library feedback");
        }
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
}
