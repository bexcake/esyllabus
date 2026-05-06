package kz.iqadam.esyllabus.web;

import java.util.List;
import kz.iqadam.esyllabus.security.CurrentUserService;
import kz.iqadam.esyllabus.syllabus.api.DisciplineCatalogItemResponse;
import kz.iqadam.esyllabus.syllabus.api.LibraryBookResponse;
import kz.iqadam.esyllabus.syllabus.service.LibraryService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/library")
public class LibraryController {

    private final CurrentUserService currentUserService;
    private final LibraryService libraryService;

    public LibraryController(CurrentUserService currentUserService, LibraryService libraryService) {
        this.currentUserService = currentUserService;
        this.libraryService = libraryService;
    }

    @GetMapping("/books")
    public List<LibraryBookResponse> searchBooks(
            @RequestParam String query,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return libraryService.searchBooks(query, limit).stream()
                .map(book -> new LibraryBookResponse(
                        book.externalId(),
                        book.title(),
                        book.author(),
                        book.year(),
                        book.url(),
                        book.type(),
                        book.discipline(),
                        book.disciplineTags(),
                        book.syncedAt()
                ))
                .toList();
    }

    @GetMapping("/disciplines")
    public List<DisciplineCatalogItemResponse> getDisciplines(
            @RequestParam(required = false) String search
    ) {
        return libraryService.getDisciplines(search);
    }

    @PostMapping("/megapro/sync")
    public LibraryService.MegaProSyncReport syncMegaPro(Authentication authentication) {
        return libraryService.syncMegaProCatalog(currentUserService.getCurrentUser(authentication));
    }

    @GetMapping(value = "/requests/export", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<ByteArrayResource> exportRequests(Authentication authentication) {
        var export = libraryService.exportRequests(currentUserService.getCurrentUser(authentication));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"library-requests.xlsx\"")
                .contentLength(export.length)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new ByteArrayResource(export));
    }
}
