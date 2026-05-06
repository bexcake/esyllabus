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
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
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
        if (schoolRepository.count() == 0) {
            schoolRepository.saveAll(List.of(
                    createSchool("school-public-policy", "SPP", "School of Public Policy", "director"),
                    createSchool("school-computing", "SCDT", "School of Computing and Digital Technologies", "professor"),
                    createSchool("school-business", "SBA", "School of Business and Analytics", "director")
            ));
        }

        if (staffProfileRepository.count() == 0) {
            staffProfileRepository.saveAll(List.of(
                    createStaff("staff-teacher", "teacher", "Aigerim Sadykova", "a.sadykova@iqadam.kz", "Main campus", "B-204", "Senior Teacher", "school-public-policy", StaffRole.TEACHER),
                    createStaff("staff-lecturer", "lecturer", "Leila Baimurat", "l.baimurat@iqadam.kz", "Innovation hub", "C-214", "Lecturer", "school-computing", StaffRole.LECTURER),
                    createStaff("staff-professor", "professor", "Arman Idrisov", "a.idrisov@iqadam.kz", "Innovation hub", "C-311", "Professor", "school-computing", StaffRole.SCHOOL_DIRECTOR),
                    createStaff("staff-director", "director", "Dana Utegenova", "d.utegenova@iqadam.kz", "Main campus", "A-101", "Director of School", "school-public-policy", StaffRole.SCHOOL_DIRECTOR),
                    createStaff("staff-librarian", "librarian", "Maira Zhanatova", "library@iqadam.kz", "Library building", "L-12", "Chief Librarian", "school-public-policy", StaffRole.LIBRARIAN)
            ));
        }

        if (studentProfileRepository.count() == 0) {
            studentProfileRepository.saveAll(List.of(
                    createStudent("student-1", "student", "Aliya Tolegen", "aliya.tolegen@iqadam.kz", 2, "CS-24-1", List.of("cs-540", "eco-214")),
                    createStudent("student-2", "student-two", "Nursultan Beken", "n.beken@iqadam.kz", 3, "BUS-23-2", List.of("bus-415", "syllabus-public-policy-2026")),
                    createStudent("student-3", "student-three", "Aruzhan Kydyr", "a.kydyr@iqadam.kz", 1, "LAW-25-1", List.of("law-331", "edu-601"))
            ));
        }
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
}
