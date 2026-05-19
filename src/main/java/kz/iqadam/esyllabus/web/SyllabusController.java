package kz.iqadam.esyllabus.web;

import com.fasterxml.jackson.databind.JsonNode;
import kz.iqadam.esyllabus.security.CurrentUser;
import kz.iqadam.esyllabus.security.CurrentUserService;
import kz.iqadam.esyllabus.syllabus.api.ImportLibraryResourcesRequest;
import kz.iqadam.esyllabus.syllabus.api.ReturnForFixRequest;
import kz.iqadam.esyllabus.syllabus.api.SyllabusCreateRequest;
import kz.iqadam.esyllabus.syllabus.api.SyllabusDirectorUpdateRequest;
import kz.iqadam.esyllabus.syllabus.api.SyllabusMetadataOptionsResponse;
import kz.iqadam.esyllabus.syllabus.api.SyllabusReviewQueueItemResponse;
import kz.iqadam.esyllabus.syllabus.api.SyllabusReviewersUpdateRequest;
import kz.iqadam.esyllabus.syllabus.api.SyllabusResponse;
import kz.iqadam.esyllabus.syllabus.service.SyllabusPdfExportService;
import kz.iqadam.esyllabus.syllabus.service.SyllabusService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/syllabi")
public class SyllabusController {

    private final CurrentUserService currentUserService;
    private final SyllabusService syllabusService;
    private final SyllabusPdfExportService syllabusPdfExportService;

    public SyllabusController(
            CurrentUserService currentUserService,
            SyllabusService syllabusService,
            SyllabusPdfExportService syllabusPdfExportService
    ) {
        this.currentUserService = currentUserService;
        this.syllabusService = syllabusService;
        this.syllabusPdfExportService = syllabusPdfExportService;
    }

    @PostMapping
    public SyllabusResponse createSyllabus(
            Authentication authentication,
            @RequestBody(required = false) SyllabusCreateRequest request
    ) {
        var payload = request == null ? new SyllabusCreateRequest(null) : request;
        return syllabusService.createSyllabus(currentUserService.getCurrentUser(authentication), payload);
    }

    @GetMapping("/review-queue")
    public java.util.List<SyllabusReviewQueueItemResponse> getReviewQueue(Authentication authentication) {
        return syllabusService.getReviewQueue(currentUserService.getCurrentUser(authentication));
    }

    @GetMapping("/{syllabusId}")
    public SyllabusResponse getSyllabus(Authentication authentication, @PathVariable String syllabusId) {
        return syllabusService.getSyllabus(currentUserService.getCurrentUser(authentication), syllabusId);
    }

    @GetMapping("/{syllabusId}/metadata-options")
    public SyllabusMetadataOptionsResponse getMetadataOptions(
            Authentication authentication,
            @PathVariable String syllabusId
    ) {
        return syllabusService.getMetadataOptions(currentUserService.getCurrentUser(authentication), syllabusId);
    }

    @PutMapping("/{syllabusId}/reviewers")
    public SyllabusResponse updateReviewers(
            Authentication authentication,
            @PathVariable String syllabusId,
            @RequestBody(required = false) SyllabusReviewersUpdateRequest request
    ) {
        return syllabusService.updateReviewers(currentUserService.getCurrentUser(authentication), syllabusId, request);
    }

    @PutMapping("/{syllabusId}/director")
    public SyllabusResponse updateDirector(
            Authentication authentication,
            @PathVariable String syllabusId,
            @RequestBody(required = false) SyllabusDirectorUpdateRequest request
    ) {
        return syllabusService.updateDirector(currentUserService.getCurrentUser(authentication), syllabusId, request);
    }

    @PutMapping("/{syllabusId}")
    public SyllabusResponse updateSyllabus(
            Authentication authentication,
            @PathVariable String syllabusId,
            @RequestBody JsonNode content
    ) {
        return syllabusService.updateSyllabus(currentUserService.getCurrentUser(authentication), syllabusId, content);
    }

    @PostMapping("/{syllabusId}/submit-review")
    public SyllabusResponse submitForReview(Authentication authentication, @PathVariable String syllabusId) {
        return syllabusService.submitForReview(currentUserService.getCurrentUser(authentication), syllabusId);
    }

    @PostMapping("/{syllabusId}/colleague-approve")
    public SyllabusResponse approveColleague(Authentication authentication, @PathVariable String syllabusId) {
        return syllabusService.approveColleague(currentUserService.getCurrentUser(authentication), syllabusId);
    }

    @PostMapping("/{syllabusId}/approve")
    public SyllabusResponse approve(Authentication authentication, @PathVariable String syllabusId) {
        return syllabusService.approve(currentUserService.getCurrentUser(authentication), syllabusId);
    }

    @PostMapping("/{syllabusId}/return-for-fix")
    public SyllabusResponse returnForFix(
            Authentication authentication,
            @PathVariable String syllabusId,
            @RequestBody(required = false) ReturnForFixRequest request
    ) {
        var comment = request == null ? "" : request.comment();
        return syllabusService.returnForFix(currentUserService.getCurrentUser(authentication), syllabusId, comment);
    }

    @PostMapping("/{syllabusId}/resources/import-from-library")
    public SyllabusResponse importResources(
            Authentication authentication,
            @PathVariable String syllabusId,
            @RequestBody ImportLibraryResourcesRequest request
    ) {
        return syllabusService.importLibraryResources(currentUserService.getCurrentUser(authentication), syllabusId, request);
    }

    @GetMapping(value = "/{syllabusId}/export-pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<ByteArrayResource> exportPdf(
            Authentication authentication,
            @PathVariable String syllabusId
    ) {
        CurrentUser currentUser = currentUserService.getCurrentUser(authentication);
        var pdf = syllabusPdfExportService.exportSyllabus(currentUser, syllabusId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + syllabusId + ".pdf\"")
                .contentLength(pdf.length)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new ByteArrayResource(pdf));
    }
}
