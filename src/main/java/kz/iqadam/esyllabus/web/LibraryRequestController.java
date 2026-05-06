package kz.iqadam.esyllabus.web;

import kz.iqadam.esyllabus.requests.api.LibraryRequestDecisionRequest;
import kz.iqadam.esyllabus.requests.api.LibraryRequestFeedbackRequest;
import kz.iqadam.esyllabus.requests.api.LibraryRequestResponse;
import kz.iqadam.esyllabus.requests.api.LibraryRequestUpsertRequest;
import kz.iqadam.esyllabus.requests.service.LibraryRequestService;
import kz.iqadam.esyllabus.security.CurrentUserService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/library/requests")
public class LibraryRequestController {

    private final CurrentUserService currentUserService;
    private final LibraryRequestService libraryRequestService;

    public LibraryRequestController(CurrentUserService currentUserService, LibraryRequestService libraryRequestService) {
        this.currentUserService = currentUserService;
        this.libraryRequestService = libraryRequestService;
    }

    @PostMapping
    public LibraryRequestResponse createRequest(
            Authentication authentication,
            @RequestBody(required = false) LibraryRequestUpsertRequest request
    ) {
        var payload = request == null ? new LibraryRequestUpsertRequest(null, null, null, java.util.List.of()) : request;
        return libraryRequestService.createRequest(currentUserService.getCurrentUser(authentication), payload);
    }

    @GetMapping
    public java.util.List<LibraryRequestResponse> getRequests(
            Authentication authentication,
            @RequestParam(required = false) String status
    ) {
        return libraryRequestService.getRequests(currentUserService.getCurrentUser(authentication), status);
    }

    @GetMapping("/{requestId}")
    public LibraryRequestResponse getRequest(Authentication authentication, @PathVariable String requestId) {
        return libraryRequestService.getRequest(currentUserService.getCurrentUser(authentication), requestId);
    }

    @PutMapping("/{requestId}")
    public LibraryRequestResponse updateRequest(
            Authentication authentication,
            @PathVariable String requestId,
            @RequestBody LibraryRequestUpsertRequest request
    ) {
        return libraryRequestService.updateRequest(currentUserService.getCurrentUser(authentication), requestId, request);
    }

    @DeleteMapping("/{requestId}")
    public void deleteRequest(Authentication authentication, @PathVariable String requestId) {
        libraryRequestService.deleteRequest(currentUserService.getCurrentUser(authentication), requestId);
    }

    @PostMapping("/{requestId}/submit")
    public LibraryRequestResponse submit(Authentication authentication, @PathVariable String requestId) {
        return libraryRequestService.submitForDirectorApproval(currentUserService.getCurrentUser(authentication), requestId);
    }

    @PostMapping("/{requestId}/director-approve")
    public LibraryRequestResponse approve(Authentication authentication, @PathVariable String requestId) {
        return libraryRequestService.approve(currentUserService.getCurrentUser(authentication), requestId);
    }

    @PostMapping("/{requestId}/director-reject")
    public LibraryRequestResponse reject(
            Authentication authentication,
            @PathVariable String requestId,
            @RequestBody(required = false) LibraryRequestDecisionRequest request
    ) {
        return libraryRequestService.reject(currentUserService.getCurrentUser(authentication), requestId, request);
    }

    @PostMapping("/{requestId}/library-feedback")
    public LibraryRequestResponse leaveFeedback(
            Authentication authentication,
            @PathVariable String requestId,
            @RequestBody LibraryRequestFeedbackRequest request
    ) {
        return libraryRequestService.leaveLibraryFeedback(currentUserService.getCurrentUser(authentication), requestId, request);
    }

    @GetMapping(value = "/{requestId}/export-form", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<ByteArrayResource> exportForm(Authentication authentication, @PathVariable String requestId) {
        var bytes = libraryRequestService.exportSingleForm(currentUserService.getCurrentUser(authentication), requestId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + requestId + ".xlsx\"")
                .contentLength(bytes.length)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new ByteArrayResource(bytes));
    }
}
