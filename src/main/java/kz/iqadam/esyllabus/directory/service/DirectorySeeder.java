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
                createSchool("school-computing", "SCDT", "School of Computing and Digital Technologies", "director-computing"),
                createSchool("school-business", "SBA", "School of Business and Analytics", "director-business"),
                createSchool("school-engineering", "SEES", "School of Engineering and Energy Systems", "director-engineering"),
                createSchool("school-health", "SHS", "School of Health Sciences", "director-health"),
                createSchool("school-education", "SEHD", "School of Education and Human Development", "director-education")
        ));

        cleanupLegacyStaffProfiles();

        seedStaff();
        seedStudents();
    }

    private void seedStaff() {
        upsertStaff(createStaff("staff-teacher", "teacher", "Aigerim Sadykova", "a.sadykova@iqadam.kz", "Main campus", "B-204", "Senior Teacher", "school-public-policy", StaffRole.TEACHER));
        upsertStaff(createStaff("staff-teacher-colleague", "teacher-colleague", "Marat Tulegenov", "m.tulegenov@iqadam.kz", "Main campus", "B-212", "Teacher", "school-public-policy", StaffRole.TEACHER));
        upsertStaff(createStaff("staff-teacher-policy-research", "teacher-policy-research", "Zarina Mukasheva", "z.mukasheva@iqadam.kz", "Main campus", "B-217", "Research Teacher", "school-public-policy", StaffRole.TEACHER));
        upsertStaff(createStaff("staff-teacher-legal", "teacher-legal", "Timur Kenzhebayev", "t.kenzhebayev@iqadam.kz", "Main campus", "B-219", "Teacher of Law", "school-public-policy", StaffRole.TEACHER));
        upsertStaff(createStaff("staff-director", "director", "Dana Utegenova", "d.utegenova@iqadam.kz", "Main campus", "A-101", "Director of School", "school-public-policy", StaffRole.SCHOOL_DIRECTOR));

        upsertStaff(createStaff("staff-teacher-digital", "teacher-digital", "Leila Baimurat", "l.baimurat@iqadam.kz", "Innovation hub", "C-214", "Teacher", "school-computing", StaffRole.TEACHER));
        upsertStaff(createStaff("staff-teacher-ai", "teacher-computing-ai", "Arman Idrisov", "a.idrisov@iqadam.kz", "Innovation hub", "C-311", "Teacher of AI", "school-computing", StaffRole.TEACHER));
        upsertStaff(createStaff("staff-teacher-software", "teacher-software-systems", "Daniyar Sarsembayev", "d.sarsembayev@iqadam.kz", "Innovation hub", "C-305", "Teacher of Software Systems", "school-computing", StaffRole.TEACHER));
        upsertStaff(createStaff("staff-teacher-cyber", "teacher-cyber", "Aruzhan Seitova", "a.seitova@iqadam.kz", "Innovation hub", "C-228", "Teacher of Cybersecurity", "school-computing", StaffRole.TEACHER));
        upsertStaff(createStaff("staff-director-computing", "director-computing", "Nurlan Zholdas", "n.zholdas@iqadam.kz", "Innovation hub", "C-101", "Director of School", "school-computing", StaffRole.SCHOOL_DIRECTOR));

        upsertStaff(createStaff("staff-teacher-business", "teacher-business", "Madina Akhmetova", "m.akhmetova@iqadam.kz", "Business center", "D-118", "Teacher", "school-business", StaffRole.TEACHER));
        upsertStaff(createStaff("staff-teacher-finance", "teacher-finance", "Askar Dulatuly", "a.dulatuly@iqadam.kz", "Business center", "D-203", "Teacher of Finance", "school-business", StaffRole.TEACHER));
        upsertStaff(createStaff("staff-teacher-operations", "teacher-operations", "Alina Saparova", "a.saparova@iqadam.kz", "Business center", "D-210", "Teacher of Operations", "school-business", StaffRole.TEACHER));
        upsertStaff(createStaff("staff-teacher-entrepreneurship", "teacher-entrepreneurship", "Rauan Zhumabek", "r.zhumabek@iqadam.kz", "Business center", "D-214", "Teacher of Entrepreneurship", "school-business", StaffRole.TEACHER));
        upsertStaff(createStaff("staff-director-business", "director-business", "Saltanat Orynbayeva", "s.orynbayeva@iqadam.kz", "Business center", "D-101", "Director of School", "school-business", StaffRole.SCHOOL_DIRECTOR));

        upsertStaff(createStaff("staff-teacher-engineering", "teacher-engineering", "Yernar Balgabayev", "y.balgabayev@iqadam.kz", "Engineering block", "E-207", "Teacher", "school-engineering", StaffRole.TEACHER));
        upsertStaff(createStaff("staff-teacher-energy", "teacher-energy", "Aida Kerimbek", "a.kerimbek@iqadam.kz", "Engineering block", "E-214", "Teacher of Energy Systems", "school-engineering", StaffRole.TEACHER));
        upsertStaff(createStaff("staff-teacher-civil", "teacher-civil", "Bekzat Omar", "b.omar@iqadam.kz", "Engineering block", "E-311", "Teacher of Civil Engineering", "school-engineering", StaffRole.TEACHER));
        upsertStaff(createStaff("staff-teacher-automation", "teacher-automation", "Miras Akhmetzhan", "m.akhmetzhan@iqadam.kz", "Engineering block", "E-322", "Teacher of Automation", "school-engineering", StaffRole.TEACHER));
        upsertStaff(createStaff("staff-director-engineering", "director-engineering", "Kairat Utepov", "k.utepov@iqadam.kz", "Engineering block", "E-101", "Director of School", "school-engineering", StaffRole.SCHOOL_DIRECTOR));

        upsertStaff(createStaff("staff-teacher-health", "teacher-health", "Ainur Zhaksylyk", "a.zhaksylyk@iqadam.kz", "Health sciences center", "H-206", "Teacher", "school-health", StaffRole.TEACHER));
        upsertStaff(createStaff("staff-teacher-pharmacy", "teacher-pharmacy", "Kamila Serikova", "k.serikova@iqadam.kz", "Health sciences center", "H-214", "Teacher of Pharmacy", "school-health", StaffRole.TEACHER));
        upsertStaff(createStaff("staff-teacher-epidemiology", "teacher-epidemiology", "Adil Beknazar", "a.beknazar@iqadam.kz", "Health sciences center", "H-228", "Teacher of Epidemiology", "school-health", StaffRole.TEACHER));
        upsertStaff(createStaff("staff-teacher-informatics", "teacher-health-informatics", "Moldir Yeleussiz", "m.yeleussiz@iqadam.kz", "Health sciences center", "H-307", "Teacher of Health Informatics", "school-health", StaffRole.TEACHER));
        upsertStaff(createStaff("staff-director-health", "director-health", "Aizhan Kurmangali", "a.kurmangali@iqadam.kz", "Health sciences center", "H-101", "Director of School", "school-health", StaffRole.SCHOOL_DIRECTOR));

        upsertStaff(createStaff("staff-teacher-education", "teacher-education", "Aizada Bekenova", "a.bekenova@iqadam.kz", "Education center", "F-205", "Teacher", "school-education", StaffRole.TEACHER));
        upsertStaff(createStaff("staff-teacher-curriculum", "teacher-curriculum", "Gulmira Tokayeva", "g.tokayeva@iqadam.kz", "Education center", "F-209", "Teacher of Curriculum Design", "school-education", StaffRole.TEACHER));
        upsertStaff(createStaff("staff-teacher-psychology", "teacher-psychology", "Nazym Rakhimova", "n.rakhimova@iqadam.kz", "Education center", "F-214", "Teacher of Educational Psychology", "school-education", StaffRole.TEACHER));
        upsertStaff(createStaff("staff-teacher-linguistics", "teacher-linguistics", "Dias Nurbek", "d.nurbek@iqadam.kz", "Education center", "F-220", "Teacher of Linguistics", "school-education", StaffRole.TEACHER));
        upsertStaff(createStaff("staff-director-education", "director-education", "Merey Altynbek", "m.altynbek@iqadam.kz", "Education center", "F-101", "Director of School", "school-education", StaffRole.SCHOOL_DIRECTOR));

        upsertStaff(createStaff("staff-librarian", "librarian", "Maira Zhanatova", "library@iqadam.kz", "Library building", "L-12", "Chief Librarian", "school-public-policy", StaffRole.LIBRARIAN));
        upsertStaff(createStaff("staff-librarian-sciences", "librarian-sciences", "Yulia Kravtsova", "library.sciences@iqadam.kz", "Library building", "L-15", "Science Librarian", "school-health", StaffRole.LIBRARIAN));
    }

    private void seedStudents() {
        upsertStudent(createStudent("student-1", "student", "Aliya Tolegen", "aliya.tolegen@iqadam.kz", 2, "school-public-policy", "Public Administration and Policy", "department-public-policy", "Department of Public Policy", "PPA-24-1", List.of("syllabus-public-policy-2026", "law-331")));
        upsertStudent(createStudent("student-2", "student-two", "Nursultan Beken", "n.beken@iqadam.kz", 3, "school-public-policy", "Public Administration and Policy", "department-public-policy", "Department of Public Policy", "PPA-23-2", List.of("syllabus-public-policy-2026", "law-331")));
        upsertStudent(createStudent("student-3", "student-three", "Aruzhan Kydyr", "a.kydyr@iqadam.kz", 1, "school-education", "Education Leadership", "department-curriculum-leadership", "Department of Curriculum and Leadership", "EDU-25-1", List.of("edu-601", "psy-410")));
        upsertStudent(createStudent("student-4", "student-computing-1", "Dias Kenzhin", "d.kenzhin@iqadam.kz", 2, "school-computing", "Computer Science", "department-data-ai", "Department of Data and AI", "CS-24-1", List.of("cs-540", "cs-575")));
        upsertStudent(createStudent("student-5", "student-computing-2", "Amina Mukhan", "a.mukhan@iqadam.kz", 1, "school-computing", "Software Engineering", "department-computer-science", "Department of Computer Science", "SE-25-1", List.of("cs-622", "cs-575")));
        upsertStudent(createStudent("student-6", "student-business-1", "Asel Kairat", "a.kairat@iqadam.kz", 4, "school-business", "Finance", "department-economics-finance", "Department of Economics and Finance", "FIN-22-1", List.of("fin-330", "eco-214")));
        upsertStudent(createStudent("student-7", "student-business-2", "Rasul Nurman", "r.nurman@iqadam.kz", 2, "school-business", "Business Administration", "department-operations-management", "Department of Operations and Management", "BUS-24-3", List.of("bus-415", "ent-410")));
        upsertStudent(createStudent("student-7b", "student-business-3", "Dana Iman", "d.iman@iqadam.kz", 2, "school-business", "Economics", "department-economics-finance", "Department of Economics and Finance", "ECO-24-2", List.of("eco-214", "fin-330")));
        upsertStudent(createStudent("student-8", "student-engineering-1", "Yasmin Ospan", "y.ospan@iqadam.kz", 3, "school-engineering", "Energy Engineering", "department-energy-systems", "Department of Energy Systems", "ENG-23-1", List.of("eng-410", "aut-360")));
        upsertStudent(createStudent("student-9", "student-engineering-2", "Sanzhar Kudaibergen", "s.kudaibergen@iqadam.kz", 2, "school-engineering", "Civil Engineering", "department-civil-automation", "Department of Civil and Automation Engineering", "CIV-24-2", List.of("civ-322", "eng-410")));
        upsertStudent(createStudent("student-10", "student-health-1", "Aigerim Tursyn", "a.tursyn@iqadam.kz", 1, "school-health", "Public Health", "department-public-health", "Department of Public Health", "HLT-25-1", List.of("epi-340", "hin-305")));
        upsertStudent(createStudent("student-11", "student-health-2", "Maksat Oraz", "m.oraz@iqadam.kz", 3, "school-health", "Pharmacy", "department-pharmacy-health-it", "Department of Pharmacy and Health Informatics", "PHR-23-1", List.of("phr-415", "hin-305")));
        upsertStudent(createStudent("student-12", "student-education-1", "Kamila Aben", "k.aben@iqadam.kz", 2, "school-education", "Educational Psychology", "department-psychology-languages", "Department of Psychology and Languages", "PSY-24-1", List.of("psy-410", "lin-350")));
        upsertStudent(createStudent("student-13", "student-education-2", "Ermek Sarsek", "e.sarsek@iqadam.kz", 4, "school-education", "Linguistics in Education", "department-psychology-languages", "Department of Psychology and Languages", "LIN-22-2", List.of("lin-350", "edu-601")));
        upsertStudent(createStudent("student-14", "student-policy-1", "Sabina Karash", "s.karash@iqadam.kz", 4, "school-public-policy", "Public Governance", "department-public-policy", "Department of Public Policy", "PPG-22-1", List.of("ppg-415", "syllabus-public-policy-2026")));
        upsertStudent(createStudent("student-15", "student-law-1", "Daniya Akhmet", "d.akhmet@iqadam.kz", 3, "school-public-policy", "Law", "department-law-governance", "Department of Law and Governance", "LAW-23-1", List.of("law-331", "ppg-415")));
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
            String schoolId,
            String programName,
            String departmentId,
            String departmentName,
            String groupName,
            List<String> currentCourseIds
    ) {
        var student = new StudentProfileEntity();
        student.setId(id);
        student.setUsername(username);
        student.setFullName(fullName);
        student.setEmail(email);
        student.setCourseNumber(courseNumber);
        student.setSchoolId(schoolId);
        student.setProgramName(programName);
        student.setDepartmentId(departmentId);
        student.setDepartmentName(departmentName);
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
        entity.setSchoolId(seed.getSchoolId());
        entity.setProgramName(seed.getProgramName());
        entity.setDepartmentId(seed.getDepartmentId());
        entity.setDepartmentName(seed.getDepartmentName());
        entity.setGroupName(seed.getGroupName());
        entity.setCurrentCourseIdsCsv(seed.getCurrentCourseIdsCsv());
        studentProfileRepository.save(entity);
    }
}
