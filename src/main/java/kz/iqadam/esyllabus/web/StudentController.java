package kz.iqadam.esyllabus.web;

import java.util.List;
import kz.iqadam.esyllabus.security.CurrentUserService;
import kz.iqadam.esyllabus.syllabus.api.CourseCatalogItemResponse;
import kz.iqadam.esyllabus.syllabus.service.SyllabusService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final CurrentUserService currentUserService;
    private final SyllabusService syllabusService;

    public StudentController(CurrentUserService currentUserService, SyllabusService syllabusService) {
        this.currentUserService = currentUserService;
        this.syllabusService = syllabusService;
    }

    @GetMapping("/me/courses")
    public List<CourseCatalogItemResponse> getCurrentStudentCourses(Authentication authentication) {
        return syllabusService.getCurrentStudentCourses(currentUserService.getCurrentUser(authentication));
    }
}
