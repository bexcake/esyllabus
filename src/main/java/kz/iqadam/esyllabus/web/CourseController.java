package kz.iqadam.esyllabus.web;

import java.util.List;
import kz.iqadam.esyllabus.security.CurrentUserService;
import kz.iqadam.esyllabus.syllabus.api.CourseCatalogItemResponse;
import kz.iqadam.esyllabus.syllabus.api.MySyllabusCardResponse;
import kz.iqadam.esyllabus.syllabus.service.SyllabusService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CourseController {

    private final CurrentUserService currentUserService;
    private final SyllabusService syllabusService;

    public CourseController(CurrentUserService currentUserService, SyllabusService syllabusService) {
        this.currentUserService = currentUserService;
        this.syllabusService = syllabusService;
    }

    @GetMapping("/courses")
    public List<CourseCatalogItemResponse> getCourses(
            Authentication authentication,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String degree,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String status
    ) {
        return syllabusService.getCourses(currentUserService.getCurrentUser(authentication), search, degree, language, status);
    }

    @GetMapping("/my-syllabi")
    public List<MySyllabusCardResponse> getMySyllabi(Authentication authentication) {
        return syllabusService.getMySyllabi(currentUserService.getCurrentUser(authentication));
    }

    @GetMapping("/courses/{courseId}")
    public CourseCatalogItemResponse getCourse(Authentication authentication, @PathVariable String courseId) {
        return syllabusService.getCourseById(currentUserService.getCurrentUser(authentication), courseId);
    }
}
