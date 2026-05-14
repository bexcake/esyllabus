package kz.iqadam.esyllabus.web;

import java.util.List;
import kz.iqadam.esyllabus.directory.api.DepartmentDirectoryResponse;
import kz.iqadam.esyllabus.directory.api.DirectoryOptionResponse;
import kz.iqadam.esyllabus.directory.api.ProgramDirectoryResponse;
import kz.iqadam.esyllabus.directory.api.SchoolResponse;
import kz.iqadam.esyllabus.directory.api.StaffPickerOptionResponse;
import kz.iqadam.esyllabus.directory.api.StaffProfileResponse;
import kz.iqadam.esyllabus.directory.api.StudentResponse;
import kz.iqadam.esyllabus.directory.service.DirectoryService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/directory")
public class DirectoryController {

    private final DirectoryService directoryService;

    public DirectoryController(DirectoryService directoryService) {
        this.directoryService = directoryService;
    }

    @GetMapping("/schools")
    public List<SchoolResponse> getSchools() {
        return directoryService.getSchools();
    }

    @GetMapping("/programs")
    public List<ProgramDirectoryResponse> getPrograms(
            @RequestParam(required = false) String schoolId,
            @RequestParam(required = false) String degreeLevel,
            @RequestParam(required = false) String search
    ) {
        return directoryService.getPrograms(schoolId, degreeLevel, search);
    }

    @GetMapping("/departments")
    public List<DepartmentDirectoryResponse> getDepartments(
            @RequestParam(required = false) String schoolId,
            @RequestParam(required = false) String search
    ) {
        return directoryService.getDepartments(schoolId, search);
    }

    @GetMapping("/academic-years")
    public List<DirectoryOptionResponse> getAcademicYears() {
        return directoryService.getAcademicYears();
    }

    @GetMapping("/trimesters")
    public List<DirectoryOptionResponse> getTrimesters() {
        return directoryService.getTrimesters();
    }

    @GetMapping("/languages")
    public List<DirectoryOptionResponse> getLanguages() {
        return directoryService.getLanguages();
    }

    @GetMapping("/degree-levels")
    public List<DirectoryOptionResponse> getDegreeLevels() {
        return directoryService.getDegreeLevels();
    }

    @GetMapping("/course-types")
    public List<DirectoryOptionResponse> getCourseTypes() {
        return directoryService.getCourseTypes();
    }

    @GetMapping("/assessment-stages")
    public List<DirectoryOptionResponse> getAssessmentStages() {
        return directoryService.getAssessmentStages();
    }

    @GetMapping("/staff")
    public List<StaffProfileResponse> getStaff(
            @RequestParam(required = false) String schoolId,
            @RequestParam(required = false) String role
    ) {
        return directoryService.getStaff(schoolId, role);
    }

    @GetMapping("/staff/picker")
    public List<StaffPickerOptionResponse> getStaffPicker(
            @RequestParam(required = false) String schoolId,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String search
    ) {
        return directoryService.getStaffPicker(schoolId, role, search);
    }

    @GetMapping("/reviewers")
    public List<StaffPickerOptionResponse> getReviewers(
            @RequestParam(required = false) String schoolId,
            @RequestParam(required = false) String syllabusId
    ) {
        return directoryService.getAllowedReviewers(schoolId, syllabusId);
    }

    @GetMapping("/staff/{username}")
    public StaffProfileResponse getStaffByUsername(@PathVariable String username) {
        return directoryService.getStaffByUsername(username);
    }

    @GetMapping("/students")
    public List<StudentResponse> getStudents(@RequestParam(required = false) String search) {
        return directoryService.getStudents(search);
    }

    @GetMapping("/students/me")
    public StudentResponse getCurrentStudent(Authentication authentication) {
        return directoryService.getCurrentStudent(authentication.getName());
    }
}
