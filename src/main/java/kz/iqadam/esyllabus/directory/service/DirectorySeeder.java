package kz.iqadam.esyllabus.directory.service;

import java.util.List;
import kz.iqadam.esyllabus.directory.model.StaffRole;
import kz.iqadam.esyllabus.directory.persistence.SchoolEntity;
import kz.iqadam.esyllabus.directory.persistence.SchoolRepository;
import kz.iqadam.esyllabus.directory.persistence.StaffProfileEntity;
import kz.iqadam.esyllabus.directory.persistence.StaffProfileRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(20)
public class DirectorySeeder implements CommandLineRunner {

    private final SchoolRepository schoolRepository;
    private final StaffProfileRepository staffProfileRepository;

    public DirectorySeeder(
            SchoolRepository schoolRepository,
            StaffProfileRepository staffProfileRepository
    ) {
        this.schoolRepository = schoolRepository;
        this.staffProfileRepository = staffProfileRepository;
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
}
