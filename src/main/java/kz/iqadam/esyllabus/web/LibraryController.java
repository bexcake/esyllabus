package kz.iqadam.esyllabus.web;

import java.util.List;
import kz.iqadam.esyllabus.syllabus.api.LibraryBookResponse;
import kz.iqadam.esyllabus.syllabus.service.LibraryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/library")
public class LibraryController {

    private final LibraryService libraryService;

    public LibraryController(LibraryService libraryService) {
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
                        book.type()
                ))
                .toList();
    }
}
