package kz.iqadam.esyllabus.directory.service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import kz.iqadam.esyllabus.directory.api.SchoolResponse;
import kz.iqadam.esyllabus.directory.api.StaffProfileResponse;
import kz.iqadam.esyllabus.directory.api.StudentResponse;
import kz.iqadam.esyllabus.directory.persistence.SchoolEntity;
import kz.iqadam.esyllabus.directory.persistence.SchoolRepository;
import kz.iqadam.esyllabus.directory.persistence.StaffProfileEntity;
import kz.iqadam.esyllabus.directory.persistence.StaffProfileRepository;
import kz.iqadam.esyllabus.directory.persistence.StudentProfileEntity;
import kz.iqadam.esyllabus.directory.persistence.StudentProfileRepository;
import kz.iqadam.esyllabus.syllabus.persistence.CourseRepository;
import kz.iqadam.esyllabus.syllabus.service.CourseMetadataSupport;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class DirectoryService {

    private final SchoolRepository schoolRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final CourseRepository courseRepository;

    public DirectoryService(
            SchoolRepository schoolRepository,
            StaffProfileRepository staffProfileRepository,
            StudentProfileRepository studentProfileRepository,
            CourseRepository courseRepository
    ) {
        this.schoolRepository = schoolRepository;
        this.staffProfileRepository = staffProfileRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.courseRepository = courseRepository;
    }

    public List<SchoolResponse> getSchools() {
        var staffBySchoolId = staffProfileRepository.findAll().stream()
                .collect(Collectors.groupingBy(StaffProfileEntity::getSchoolId));
        var staffByUsername = staffProfileRepository.findAll().stream()
                .collect(Collectors.toMap(StaffProfileEntity::getUsername, Function.identity(), (left, right) -> left));

        return schoolRepository.findAll().stream()
                .sorted(Comparator.comparing(SchoolEntity::getName))
                .map(school -> new SchoolResponse(
                        school.getId(),
                        school.getCode(),
                        school.getName(),
                        school.getDirectorUsername(),
                        Optional.ofNullable(staffByUsername.get(school.getDirectorUsername()))
                                .map(StaffProfileEntity::getFullName)
                                .orElse(school.getDirectorUsername()),
                        staffBySchoolId.getOrDefault(school.getId(), List.of()).size()
                ))
                .toList();
    }

    public List<StaffProfileResponse> getStaff(String schoolId, String role) {
        var schoolNames = schoolRepository.findAll().stream()
                .collect(Collectors.toMap(SchoolEntity::getId, SchoolEntity::getName));

        return staffProfileRepository.findAll().stream()
                .filter(item -> normalized(schoolId) == null || item.getSchoolId().equalsIgnoreCase(schoolId.trim()))
                .filter(item -> normalized(role) == null || item.getRole().name().equalsIgnoreCase(role.trim()))
                .sorted(Comparator.comparing(StaffProfileEntity::getFullName))
                .map(item -> new StaffProfileResponse(
                        item.getId(),
                        item.getUsername(),
                        item.getFullName(),
                        item.getEmail(),
                        item.getWorkplace(),
                        item.getCabinet(),
                        item.getPositionTitle(),
                        item.getSchoolId(),
                        schoolNames.getOrDefault(item.getSchoolId(), item.getSchoolId()),
                        item.getRole().name()
                ))
                .toList();
    }

    public List<StudentResponse> getStudents(String search) {
        var coursesById = courseRepository.findAll().stream()
                .collect(Collectors.toMap(course -> course.getId(), Function.identity(), (left, right) -> left));

        return studentProfileRepository.findAll().stream()
                .filter(student -> normalized(search) == null
                        || (student.getFullName() + " " + student.getGroupName()).toLowerCase(Locale.ROOT)
                        .contains(search.trim().toLowerCase(Locale.ROOT)))
                .sorted(Comparator.comparing(StudentProfileEntity::getFullName))
                .map(student -> new StudentResponse(
                        student.getId(),
                        student.getFullName(),
                        student.getCourseNumber(),
                        student.getGroupName(),
                        CourseMetadataSupport.parseCsv(student.getCurrentCourseIdsCsv()).stream()
                                .map(coursesById::get)
                                .filter(Objects::nonNull)
                                .map(course -> new StudentResponse.CurrentCourseResponse(
                                        course.getId(),
                                        course.getCode(),
                                        course.getTitle()
                                ))
                                .toList()
                ))
                .toList();
    }

    public StaffProfileEntity getRequiredStaffProfile(String username) {
        return staffProfileRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Current user is not mapped to staff directory"));
    }

    public SchoolEntity getRequiredSchool(String schoolId) {
        return schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "School not found"));
    }

    public Map<String, SchoolEntity> getSchoolsById() {
        return schoolRepository.findAll().stream()
                .collect(Collectors.toMap(SchoolEntity::getId, Function.identity(), (left, right) -> left));
    }

    public Map<String, StaffProfileEntity> getStaffByUsername() {
        return staffProfileRepository.findAll().stream()
                .collect(Collectors.toMap(StaffProfileEntity::getUsername, Function.identity(), (left, right) -> left));
    }

    private String normalized(String value) {
        if (value == null) {
            return null;
        }
        var result = value.trim();
        return result.isBlank() ? null : result;
    }
}
