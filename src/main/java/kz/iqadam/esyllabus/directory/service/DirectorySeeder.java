package kz.iqadam.esyllabus.directory.service;

import java.util.List;
import kz.iqadam.esyllabus.directory.model.StaffRole;
import kz.iqadam.esyllabus.directory.persistence.SchoolEntity;
import kz.iqadam.esyllabus.directory.persistence.SchoolRepository;
import kz.iqadam.esyllabus.directory.persistence.StaffProfileEntity;
import kz.iqadam.esyllabus.directory.persistence.StaffProfileRepository;
import kz.iqadam.esyllabus.directory.persistence.StudentProfileEntity;
import kz.iqadam.esyllabus.directory.persistence.StudentProfileRepository;
import kz.iqadam.esyllabus.syllabus.service.CourseMetadataSupport;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(20)
public class DirectorySeeder implements CommandLineRunner {

    private final SchoolRepository schoolRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final StudentProfileRepository studentProfileRepository;

    public DirectorySeeder(
            SchoolRepository schoolRepository,
            StaffProfileRepository staffProfileRepository,
            StudentProfileRepository studentProfileRepository
    ) {
        this.schoolRepository = schoolRepository;
        this.staffProfileRepository = staffProfileRepository;
        this.studentProfileRepository = studentProfileRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        schoolRepository.saveAll(List.of(
                createSchool("school-public-policy", "SPP", "School of Public Policy", "director"),
                createSchool("school-computing", "SCDT", "School of Computing and Digital Technologies", "director"),
                createSchool("school-business", "SBA", "School of Business and Analytics", "director")
        ));

        cleanupLegacyStaffProfiles();

        upsertStaff(createStaff("staff-teacher", "teacher", "Aigerim Sadykova", "a.sadykova@iqadam.kz", "Main campus", "B-204", "Senior Teacher", "school-public-policy", StaffRole.TEACHER));
        upsertStaff(createStaff("staff-teacher-colleague", "teacher-colleague", "Marat Tulegenov", "m.tulegenov@iqadam.kz", "Main campus", "B-212", "Teacher", "school-public-policy", StaffRole.TEACHER));
        upsertStaff(createStaff("staff-teacher-digital", "teacher-digital", "Leila Baimurat", "l.baimurat@iqadam.kz", "Innovation hub", "C-214", "Teacher", "school-computing", StaffRole.TEACHER));
        upsertStaff(createStaff("staff-director", "director", "Dana Utegenova", "d.utegenova@iqadam.kz", "Main campus", "A-101", "Director of School", "school-public-policy", StaffRole.SCHOOL_DIRECTOR));
        upsertStaff(createStaff("staff-librarian", "librarian", "Maira Zhanatova", "library@iqadam.kz", "Library building", "L-12", "Chief Librarian", "school-public-policy", StaffRole.LIBRARIAN));

        upsertStudent(createStudent("student-1", "student", "Aliya Tolegen", "aliya.tolegen@iqadam.kz", 2, "CS-24-1", List.of("cs-540", "eco-214")));
        upsertStudent(createStudent("student-2", "student-two", "Nursultan Beken", "n.beken@iqadam.kz", 3, "BUS-23-2", List.of("bus-415", "syllabus-public-policy-2026")));
        upsertStudent(createStudent("student-3", "student-three", "Aruzhan Kydyr", "a.kydyr@iqadam.kz", 1, "LAW-25-1", List.of("law-331", "edu-601")));
    }

    private SchoolEntity createSchool(String id, String code, String name, String directorUsername) {
        var school = new SchoolEntity();
        school.setId(id);
        school.setCode(code);
        school.setName(name);
        school.setDirectorUsername(directorUsername);
        return school;
    }

    private StaffProfileEntity createStaff(
            String id,
            String username,
            String fullName,
            String email,
            String workplace,
            String cabinet,
            String positionTitle,
            String schoolId,
            StaffRole role
    ) {
        var staff = new StaffProfileEntity();
        staff.setId(id);
        staff.setUsername(username);
        staff.setFullName(fullName);
        staff.setEmail(email);
        staff.setWorkplace(workplace);
        staff.setCabinet(cabinet);
        staff.setPositionTitle(positionTitle);
        staff.setSchoolId(schoolId);
        staff.setRole(role);
        return staff;
    }

    private StudentProfileEntity createStudent(
            String id,
            String username,
            String fullName,
            String email,
            int courseNumber,
            String groupName,
            List<String> currentCourseIds
    ) {
        var student = new StudentProfileEntity();
        student.setId(id);
        student.setUsername(username);
        student.setFullName(fullName);
        student.setEmail(email);
        student.setCourseNumber(courseNumber);
        student.setGroupName(groupName);
        student.setCurrentCourseIdsCsv(CourseMetadataSupport.toCsv(currentCourseIds));
        return student;
    }

    private void cleanupLegacyStaffProfiles() {
        staffProfileRepository.findByUsername("professor")
                .ifPresent(staffProfileRepository::delete);
        staffProfileRepository.findByUsername("lecturer")
                .ifPresent(staffProfileRepository::delete);
    }

    private void upsertStaff(StaffProfileEntity seed) {
        var entity = staffProfileRepository.findByUsername(seed.getUsername()).orElseGet(StaffProfileEntity::new);
        entity.setId(seed.getId());
        entity.setUsername(seed.getUsername());
        entity.setFullName(seed.getFullName());
        entity.setEmail(seed.getEmail());
        entity.setWorkplace(seed.getWorkplace());
        entity.setCabinet(seed.getCabinet());
        entity.setPositionTitle(seed.getPositionTitle());
        entity.setSchoolId(seed.getSchoolId());
        entity.setRole(seed.getRole());
        staffProfileRepository.save(entity);
    }

    private void upsertStudent(StudentProfileEntity seed) {
        var entity = studentProfileRepository.findByUsername(seed.getUsername()).orElseGet(StudentProfileEntity::new);
        entity.setId(seed.getId());
        entity.setUsername(seed.getUsername());
        entity.setFullName(seed.getFullName());
        entity.setEmail(seed.getEmail());
        entity.setCourseNumber(seed.getCourseNumber());
        entity.setGroupName(seed.getGroupName());
        entity.setCurrentCourseIdsCsv(seed.getCurrentCourseIdsCsv());
        studentProfileRepository.save(entity);
    }
}
