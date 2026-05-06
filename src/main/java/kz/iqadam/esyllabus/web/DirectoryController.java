package kz.iqadam.esyllabus.web;

import java.util.List;
import kz.iqadam.esyllabus.directory.api.SchoolResponse;
import kz.iqadam.esyllabus.directory.api.StaffProfileResponse;
import kz.iqadam.esyllabus.directory.api.StudentResponse;
import kz.iqadam.esyllabus.directory.service.DirectoryService;
import org.springframework.web.bind.annotation.GetMapping;
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

    @GetMapping("/staff")
    public List<StaffProfileResponse> getStaff(
            @RequestParam(required = false) String schoolId,
            @RequestParam(required = false) String role
    ) {
        return directoryService.getStaff(schoolId, role);
    }

    @GetMapping("/students")
    public List<StudentResponse> getStudents(@RequestParam(required = false) String search) {
        return directoryService.getStudents(search);
    }
}
