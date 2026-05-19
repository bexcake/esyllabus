package kz.iqadam.esyllabus.directory.service;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import kz.iqadam.esyllabus.directory.api.DirectoryOptionResponse;
import kz.iqadam.esyllabus.directory.api.ProgramDirectoryResponse;
import kz.iqadam.esyllabus.directory.api.SchoolResponse;
import kz.iqadam.esyllabus.directory.api.StaffPickerOptionResponse;
import kz.iqadam.esyllabus.directory.api.StaffProfileResponse;
import kz.iqadam.esyllabus.directory.model.StaffRole;
import kz.iqadam.esyllabus.directory.persistence.SchoolEntity;
import kz.iqadam.esyllabus.directory.persistence.SchoolRepository;
import kz.iqadam.esyllabus.directory.persistence.StaffProfileEntity;
import kz.iqadam.esyllabus.directory.persistence.StaffProfileRepository;
import kz.iqadam.esyllabus.syllabus.persistence.CourseEntity;
import kz.iqadam.esyllabus.syllabus.persistence.CourseRepository;
import kz.iqadam.esyllabus.syllabus.persistence.SyllabusRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class DirectoryService {

    private static final List<DirectoryOptionResponse> DEGREE_LEVELS = List.of(
            option("Bachelor"),
            option("Master"),
            option("PhD"),
            option("Certificate")
    );

    private static final List<DirectoryOptionResponse> COURSE_TYPES = List.of(
            option("Compulsory"),
            option("Elective"),
            option("University requirement"),
            option("Program requirement")
    );

    private static final List<DirectoryOptionResponse> ASSESSMENT_STAGES = List.of(
            option("Continuous assessment"),
            option("First attestation"),
            option("Second attestation"),
            option("Final assessment")
    );

    private static final List<String> TRIMESTER_ORDER = List.of("Autumn", "Spring", "Summer", "Winter");
    private static final List<String> LANGUAGE_ORDER = List.of("English", "Kazakh", "Russian");

    private final SchoolRepository schoolRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final CourseRepository courseRepository;
    private final SyllabusRepository syllabusRepository;

    public DirectoryService(
            SchoolRepository schoolRepository,
            StaffProfileRepository staffProfileRepository,
            CourseRepository courseRepository,
            SyllabusRepository syllabusRepository
    ) {
        this.schoolRepository = schoolRepository;
        this.staffProfileRepository = staffProfileRepository;
        this.courseRepository = courseRepository;
        this.syllabusRepository = syllabusRepository;
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

    public List<ProgramDirectoryResponse> getPrograms(String schoolId, String degreeLevel, String search) {
        var schoolNames = getSchoolNames();
        var normalizedSchoolId = normalized(schoolId);
        var normalizedDegreeLevel = normalized(degreeLevel);
        var normalizedSearch = normalized(search);

        return courseRepository.findAll().stream()
                .filter(course -> normalizedSchoolId == null || normalizedSchoolId.equalsIgnoreCase(course.getSchoolId()))
                .filter(course -> normalizedDegreeLevel == null || normalizedDegreeLevel.equalsIgnoreCase(course.getDegreeLevel()))
                .collect(Collectors.groupingBy(
                        course -> new ProgramKey(course.getProgram(), course.getSchoolId(), course.getDegreeLevel()),
                        LinkedHashMap::new,
                        Collectors.toList()
                ))
                .entrySet().stream()
                .map(entry -> toProgramResponse(entry.getKey(), entry.getValue(), schoolNames))
                .filter(program -> normalizedSearch == null || searchable(program).contains(normalizedSearch.toLowerCase(Locale.ROOT)))
                .sorted(Comparator.comparing(ProgramDirectoryResponse::schoolName).thenComparing(ProgramDirectoryResponse::name))
                .toList();
    }

    public List<DirectoryOptionResponse> getAcademicYears() {
        var values = courseRepository.findAll().stream()
                .map(CourseEntity::getAcademicYear)
                .map(this::normalized)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (values.isEmpty()) {
            values.add("2026-2027");
        }
        return values.stream()
                .sorted(Comparator.reverseOrder())
                .map(DirectoryService::option)
                .toList();
    }

    public List<DirectoryOptionResponse> getTrimesters() {
        return orderedOptions(
                courseRepository.findAll().stream()
                        .map(CourseEntity::getTrimester)
                        .toList(),
                TRIMESTER_ORDER
        );
    }

    public List<DirectoryOptionResponse> getLanguages() {
        return orderedOptions(
                courseRepository.findAll().stream()
                        .map(CourseEntity::getLanguageOfInstruction)
                        .toList(),
                LANGUAGE_ORDER
        );
    }

    public List<DirectoryOptionResponse> getDegreeLevels() {
        return DEGREE_LEVELS;
    }

    public List<DirectoryOptionResponse> getCourseTypes() {
        return COURSE_TYPES;
    }

    public List<DirectoryOptionResponse> getAssessmentStages() {
        return ASSESSMENT_STAGES;
    }

    public List<StaffProfileResponse> getStaff(String schoolId, String role) {
        var schoolNames = getSchoolNames();
        var normalizedRole = normalized(role);

        return staffProfileRepository.findAll().stream()
                .filter(item -> normalized(schoolId) == null || item.getSchoolId().equalsIgnoreCase(schoolId.trim()))
                .filter(item -> normalizedRole == null
                        || item.getRole().apiValue().equalsIgnoreCase(normalizedRole)
                        || item.getRole().name().equalsIgnoreCase(normalizedRole))
                .sorted(Comparator.comparing(StaffProfileEntity::getFullName))
                .map(item -> toStaffProfileResponse(item, schoolNames))
                .toList();
    }

    public StaffProfileResponse getStaffByUsername(String username) {
        var staff = staffProfileRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff profile not found"));
        return toStaffProfileResponse(staff, getSchoolNames());
    }

    public List<StaffPickerOptionResponse> getStaffPicker(String schoolId, String role, String search) {
        var schoolNames = getSchoolNames();
        var normalizedSchoolId = normalized(schoolId);
        var normalizedRole = normalized(role);
        var normalizedSearch = normalized(search);

        return staffProfileRepository.findAll().stream()
                .filter(item -> normalizedSchoolId == null || item.getSchoolId().equalsIgnoreCase(normalizedSchoolId))
                .filter(item -> normalizedRole == null
                        || item.getRole().apiValue().equalsIgnoreCase(normalizedRole)
                        || item.getRole().name().equalsIgnoreCase(normalizedRole))
                .filter(item -> normalizedSearch == null || searchable(item).contains(normalizedSearch.toLowerCase(Locale.ROOT)))
                .sorted(Comparator.comparing(StaffProfileEntity::getFullName))
                .map(item -> toStaffPickerOption(item, schoolNames.getOrDefault(item.getSchoolId(), item.getSchoolId())))
                .toList();
    }

    public List<StaffPickerOptionResponse> getAllowedReviewers(String schoolId, String syllabusId) {
        var resolvedSyllabusId = normalized(syllabusId);
        var resolvedSchoolId = normalized(schoolId);
        String excludedUsername = null;

        if (resolvedSyllabusId != null) {
            var syllabus = syllabusRepository.findById(resolvedSyllabusId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Syllabus not found"));
            excludedUsername = syllabus.getOwnerEmail();
            resolvedSchoolId = getRequiredStaffProfile(syllabus.getOwnerEmail()).getSchoolId();
        }

        final String effectiveSchoolId = resolvedSchoolId;
        final String effectiveExcludedUsername = excludedUsername;
        var schoolNames = getSchoolNames();
        var results = new LinkedHashMap<String, StaffPickerOptionResponse>();

        staffProfileRepository.findAll().stream()
                .filter(item -> effectiveSchoolId == null || item.getSchoolId().equalsIgnoreCase(effectiveSchoolId))
                .filter(item -> item.getRole() != StaffRole.LIBRARIAN)
                .filter(item -> item.getRole() != StaffRole.SCHOOL_DIRECTOR)
                .filter(item -> effectiveExcludedUsername == null || !item.getUsername().equalsIgnoreCase(effectiveExcludedUsername))
                .sorted(Comparator.comparing(StaffProfileEntity::getFullName))
                .map(item -> toStaffPickerOption(item, schoolNames.getOrDefault(item.getSchoolId(), item.getSchoolId())))
                .forEach(item -> results.put(item.username(), item));

        return results.values().stream()
                .sorted(Comparator.comparing(StaffPickerOptionResponse::fullName))
                .toList();
    }

    public List<StaffPickerOptionResponse> getAllowedDirectors(String schoolId) {
        var schoolNames = getSchoolNames();
        var normalizedSchoolId = normalized(schoolId);

        return staffProfileRepository.findAll().stream()
                .filter(item -> normalizedSchoolId == null || item.getSchoolId().equalsIgnoreCase(normalizedSchoolId))
                .filter(item -> item.getRole() == StaffRole.SCHOOL_DIRECTOR)
                .sorted(Comparator.comparing(StaffProfileEntity::getFullName))
                .map(item -> toStaffPickerOption(item, schoolNames.getOrDefault(item.getSchoolId(), item.getSchoolId())))
                .toList();
    }

    public List<StaffPickerOptionResponse> getAllowedInstructors(String schoolId) {
        var schoolNames = getSchoolNames();
        var normalizedSchoolId = normalized(schoolId);

        return staffProfileRepository.findAll().stream()
                .filter(item -> normalizedSchoolId == null || item.getSchoolId().equalsIgnoreCase(normalizedSchoolId))
                .filter(item -> item.getRole().isTeachingStaff())
                .sorted(Comparator.comparing(StaffProfileEntity::getFullName))
                .map(item -> toStaffPickerOption(item, schoolNames.getOrDefault(item.getSchoolId(), item.getSchoolId())))
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

    public List<StaffProfileEntity> getStaffByUsernames(List<String> usernames) {
        var requested = usernames == null ? Set.<String>of() : usernames.stream()
                .map(this::normalized)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (requested.isEmpty()) {
            return List.of();
        }
        return staffProfileRepository.findAll().stream()
                .filter(item -> requested.contains(item.getUsername()))
                .sorted(Comparator.comparing(StaffProfileEntity::getFullName))
                .toList();
    }

    private ProgramDirectoryResponse toProgramResponse(
            ProgramKey key,
            List<CourseEntity> courses,
            Map<String, String> schoolNames
    ) {
        var sample = courses.stream()
                .sorted(Comparator.comparing(CourseEntity::getCode))
                .findFirst()
                .orElseThrow();
        return new ProgramDirectoryResponse(
                "program-" + slug(key.schoolId() + "-" + key.degreeLevel() + "-" + key.name()),
                deriveProgramCode(sample.getCode(), key.name()),
                key.name(),
                key.degreeLevel(),
                key.schoolId(),
                schoolNames.getOrDefault(key.schoolId(), key.schoolId())
        );
    }

    private StaffProfileResponse toStaffProfileResponse(StaffProfileEntity item, Map<String, String> schoolNames) {
        return new StaffProfileResponse(
                item.getId(),
                item.getUsername(),
                item.getFullName(),
                item.getEmail(),
                item.getWorkplace(),
                item.getCabinet(),
                item.getPositionTitle(),
                item.getSchoolId(),
                schoolNames.getOrDefault(item.getSchoolId(), item.getSchoolId()),
                item.getRole().apiValue()
        );
    }

    private StaffPickerOptionResponse toStaffPickerOption(StaffProfileEntity item, String schoolName) {
        return new StaffPickerOptionResponse(
                item.getUsername(),
                item.getFullName(),
                item.getEmail(),
                item.getPositionTitle(),
                item.getSchoolId(),
                schoolName,
                item.getRole().apiValue()
        );
    }

    private Map<String, String> getSchoolNames() {
        return schoolRepository.findAll().stream()
                .collect(Collectors.toMap(SchoolEntity::getId, SchoolEntity::getName, (left, right) -> left));
    }

    private List<DirectoryOptionResponse> orderedOptions(List<String> rawValues, List<String> preferredOrder) {
        var values = rawValues.stream()
                .map(this::normalized)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (values.isEmpty()) {
            values.addAll(preferredOrder);
        }

        var result = new ArrayList<DirectoryOptionResponse>();
        for (var value : preferredOrder) {
            if (values.remove(value)) {
                result.add(option(value));
            }
        }
        values.stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .map(DirectoryService::option)
                .forEach(result::add);
        return List.copyOf(result);
    }

    private String deriveProgramCode(String courseCode, String programName) {
        var normalizedCourseCode = normalized(courseCode);
        if (normalizedCourseCode != null) {
            var parts = normalizedCourseCode.split("\\s+");
            if (parts.length > 0 && !parts[0].isBlank()) {
                return parts[0].toUpperCase(Locale.ROOT);
            }
        }

        var initials = Arrays.stream(programName.split("\\s+"))
                .map(token -> token.replaceAll("[^\\p{L}\\p{Nd}]", ""))
                .filter(token -> !token.isBlank())
                .map(token -> token.substring(0, 1).toUpperCase(Locale.ROOT))
                .collect(Collectors.joining());
        return initials.isBlank() ? "PRG" : initials;
    }

    private String searchable(ProgramDirectoryResponse program) {
        return (program.code() + " " + program.name() + " " + program.schoolName())
                .toLowerCase(Locale.ROOT);
    }

    private String searchable(StaffProfileEntity staff) {
        return (staff.getUsername() + " " + staff.getFullName() + " " + staff.getEmail() + " "
                + Objects.toString(staff.getPositionTitle(), ""))
                .toLowerCase(Locale.ROOT);
    }

    private String slug(String value) {
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    private String normalized(String value) {
        if (value == null) {
            return null;
        }
        var result = value.trim();
        return result.isBlank() ? null : result;
    }

    private static DirectoryOptionResponse option(String value) {
        return new DirectoryOptionResponse(value, value);
    }

    private record ProgramKey(
            String name,
            String schoolId,
            String degreeLevel
    ) {
    }
}
