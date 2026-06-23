package kz.iqadam.esyllabus.workflow;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import kz.iqadam.esyllabus.directory.model.StaffRole;
import kz.iqadam.esyllabus.directory.persistence.SchoolEntity;
import kz.iqadam.esyllabus.directory.persistence.SchoolRepository;
import kz.iqadam.esyllabus.directory.persistence.StaffProfileEntity;
import kz.iqadam.esyllabus.directory.persistence.StaffProfileRepository;
import kz.iqadam.esyllabus.integration.megapro.MegaProResourceCacheEntity;
import kz.iqadam.esyllabus.integration.megapro.MegaProResourceCacheRepository;
import kz.iqadam.esyllabus.syllabus.persistence.CourseEntity;
import kz.iqadam.esyllabus.syllabus.persistence.CourseRepository;
import kz.iqadam.esyllabus.syllabus.service.CourseMetadataSupport;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
class WorkflowTestFixturesConfiguration {

    @Bean
    ApplicationRunner workflowTestFixtures(
            SchoolRepository schoolRepository,
            StaffProfileRepository staffProfileRepository,
            CourseRepository courseRepository,
            MegaProResourceCacheRepository megaProResourceCacheRepository
    ) {
        return args -> {
            seedSchools(schoolRepository);
            seedStaff(staffProfileRepository);
            seedCourses(courseRepository);
            seedMegaProCache(courseRepository, megaProResourceCacheRepository);
        };
    }

    private void seedSchools(SchoolRepository repository) {
        repository.saveAll(List.of(
                school("school-public-policy", "SPP", "School of Public Policy", "director"),
                school("school-computing", "SCDT", "School of Computing and Digital Technologies", "director-computing"),
                school("school-business", "SBA", "School of Business and Analytics", "director-business"),
                school("school-engineering", "SEES", "School of Engineering and Energy Systems", "director-engineering"),
                school("school-health", "SHS", "School of Health Sciences", "director-health"),
                school("school-education", "SEHD", "School of Education and Human Development", "director-education")
        ));
    }

    private void seedStaff(StaffProfileRepository repository) {
        List.of(
                staff("staff-teacher", "teacher", "Aigerim Sadykova", "a.sadykova@iqadam.kz", "Main campus", "B-204", "Senior Teacher", "school-public-policy", StaffRole.TEACHER),
                staff("staff-teacher-colleague", "teacher-colleague", "Marat Tulegenov", "m.tulegenov@iqadam.kz", "Main campus", "B-212", "Teacher", "school-public-policy", StaffRole.TEACHER),
                staff("staff-teacher-policy-research", "teacher-policy-research", "Zarina Mukasheva", "z.mukasheva@iqadam.kz", "Main campus", "B-217", "Research Teacher", "school-public-policy", StaffRole.TEACHER),
                staff("staff-teacher-legal", "teacher-legal", "Timur Kenzhebayev", "t.kenzhebayev@iqadam.kz", "Main campus", "B-219", "Teacher of Law", "school-public-policy", StaffRole.TEACHER),
                staff("staff-director", "director", "Dana Utegenova", "d.utegenova@iqadam.kz", "Main campus", "A-101", "Director of School", "school-public-policy", StaffRole.SCHOOL_DIRECTOR),
                staff("staff-teacher-digital", "teacher-digital", "Leila Baimurat", "l.baimurat@iqadam.kz", "Innovation hub", "C-214", "Teacher", "school-computing", StaffRole.TEACHER),
                staff("staff-teacher-ai", "teacher-computing-ai", "Arman Idrisov", "a.idrisov@iqadam.kz", "Innovation hub", "C-311", "Teacher of AI", "school-computing", StaffRole.TEACHER),
                staff("staff-teacher-software", "teacher-software-systems", "Daniyar Sarsembayev", "d.sarsembayev@iqadam.kz", "Innovation hub", "C-305", "Teacher of Software Systems", "school-computing", StaffRole.TEACHER),
                staff("staff-teacher-cyber", "teacher-cyber", "Aruzhan Seitova", "a.seitova@iqadam.kz", "Innovation hub", "C-228", "Teacher of Cybersecurity", "school-computing", StaffRole.TEACHER),
                staff("staff-director-computing", "director-computing", "Nurlan Zholdas", "n.zholdas@iqadam.kz", "Innovation hub", "C-101", "Director of School", "school-computing", StaffRole.SCHOOL_DIRECTOR),
                staff("staff-teacher-business", "teacher-business", "Madina Akhmetova", "m.akhmetova@iqadam.kz", "Business center", "D-118", "Teacher", "school-business", StaffRole.TEACHER),
                staff("staff-teacher-finance", "teacher-finance", "Askar Dulatuly", "a.dulatuly@iqadam.kz", "Business center", "D-203", "Teacher of Finance", "school-business", StaffRole.TEACHER),
                staff("staff-teacher-operations", "teacher-operations", "Alina Saparova", "a.saparova@iqadam.kz", "Business center", "D-210", "Teacher of Operations", "school-business", StaffRole.TEACHER),
                staff("staff-teacher-entrepreneurship", "teacher-entrepreneurship", "Rauan Zhumabek", "r.zhumabek@iqadam.kz", "Business center", "D-214", "Teacher of Entrepreneurship", "school-business", StaffRole.TEACHER),
                staff("staff-director-business", "director-business", "Saltanat Orynbayeva", "s.orynbayeva@iqadam.kz", "Business center", "D-101", "Director of School", "school-business", StaffRole.SCHOOL_DIRECTOR),
                staff("staff-teacher-engineering", "teacher-engineering", "Yernar Balgabayev", "y.balgabayev@iqadam.kz", "Engineering block", "E-207", "Teacher", "school-engineering", StaffRole.TEACHER),
                staff("staff-teacher-energy", "teacher-energy", "Aida Kerimbek", "a.kerimbek@iqadam.kz", "Engineering block", "E-214", "Teacher of Energy Systems", "school-engineering", StaffRole.TEACHER),
                staff("staff-teacher-civil", "teacher-civil", "Bekzat Omar", "b.omar@iqadam.kz", "Engineering block", "E-311", "Teacher of Civil Engineering", "school-engineering", StaffRole.TEACHER),
                staff("staff-teacher-automation", "teacher-automation", "Miras Akhmetzhan", "m.akhmetzhan@iqadam.kz", "Engineering block", "E-322", "Teacher of Automation", "school-engineering", StaffRole.TEACHER),
                staff("staff-director-engineering", "director-engineering", "Kairat Utepov", "k.utepov@iqadam.kz", "Engineering block", "E-101", "Director of School", "school-engineering", StaffRole.SCHOOL_DIRECTOR),
                staff("staff-teacher-health", "teacher-health", "Ainur Zhaksylyk", "a.zhaksylyk@iqadam.kz", "Health sciences center", "H-206", "Teacher", "school-health", StaffRole.TEACHER),
                staff("staff-teacher-pharmacy", "teacher-pharmacy", "Kamila Serikova", "k.serikova@iqadam.kz", "Health sciences center", "H-214", "Teacher of Pharmacy", "school-health", StaffRole.TEACHER),
                staff("staff-teacher-epidemiology", "teacher-epidemiology", "Adil Beknazar", "a.beknazar@iqadam.kz", "Health sciences center", "H-228", "Teacher of Epidemiology", "school-health", StaffRole.TEACHER),
                staff("staff-teacher-informatics", "teacher-health-informatics", "Moldir Yeleussiz", "m.yeleussiz@iqadam.kz", "Health sciences center", "H-307", "Teacher of Health Informatics", "school-health", StaffRole.TEACHER),
                staff("staff-director-health", "director-health", "Aizhan Kurmangali", "a.kurmangali@iqadam.kz", "Health sciences center", "H-101", "Director of School", "school-health", StaffRole.SCHOOL_DIRECTOR),
                staff("staff-teacher-education", "teacher-education", "Aizada Bekenova", "a.bekenova@iqadam.kz", "Education center", "F-205", "Teacher", "school-education", StaffRole.TEACHER),
                staff("staff-teacher-curriculum", "teacher-curriculum", "Gulmira Tokayeva", "g.tokayeva@iqadam.kz", "Education center", "F-209", "Teacher of Curriculum Design", "school-education", StaffRole.TEACHER),
                staff("staff-teacher-psychology", "teacher-psychology", "Nazym Rakhimova", "n.rakhimova@iqadam.kz", "Education center", "F-214", "Teacher of Educational Psychology", "school-education", StaffRole.TEACHER),
                staff("staff-teacher-linguistics", "teacher-linguistics", "Dias Nurbek", "d.nurbek@iqadam.kz", "Education center", "F-220", "Teacher of Linguistics", "school-education", StaffRole.TEACHER),
                staff("staff-director-education", "director-education", "Merey Altynbek", "m.altynbek@iqadam.kz", "Education center", "F-101", "Director of School", "school-education", StaffRole.SCHOOL_DIRECTOR),
                staff("staff-librarian", "librarian", "Maira Zhanatova", "library@iqadam.kz", "Library building", "L-12", "Chief Librarian", "school-public-policy", StaffRole.LIBRARIAN),
                staff("staff-librarian-sciences", "librarian-sciences", "Yulia Kravtsova", "library.sciences@iqadam.kz", "Library building", "L-15", "Science Librarian", "school-health", StaffRole.LIBRARIAN)
        ).forEach(seed -> upsertStaff(repository, seed));
    }

    private void seedCourses(CourseRepository repository) {
        List.of(
                course("syllabus-public-policy-2026", "Public Policy Analysis and Design", "PPA 302", "Public Administration and Policy", "school-public-policy", "Bachelor", "2026-2027", "Spring", "English", 6, List.of("Aigerim Sadykova", "Marat Tulegenov"), List.of("public policy", "policy analysis", "governance")),
                course("law-331", "Comparative Constitutional Law", "LAW 331", "Law", "school-public-policy", "Bachelor", "2026-2027", "Spring", "English", 6, List.of("Timur Kenzhebayev", "Zarina Mukasheva"), List.of("constitutional law", "comparative law", "legal systems")),
                course("ppg-415", "Sustainable Urban Governance", "PPG 415", "Public Governance", "school-public-policy", "Master", "2026-2027", "Autumn", "English", 5, List.of("Dana Utegenova", "Aigerim Sadykova"), List.of("urban governance", "sustainability", "public management")),
                course("cs-540", "Applied Machine Learning Studio", "CS 540", "Computer Science", "school-computing", "Master", "2026-2027", "Spring", "English", 7, List.of("Arman Idrisov", "Leila Baimurat"), List.of("machine learning", "artificial intelligence", "data science")),
                course("cs-622", "Cloud Systems Engineering", "CS 622", "Software Engineering", "school-computing", "Master", "2026-2027", "Autumn", "English", 6, List.of("Daniyar Sarsembayev", "Leila Baimurat"), List.of("cloud computing", "distributed systems", "software engineering")),
                course("cs-575", "Cybersecurity Governance", "CS 575", "Cybersecurity", "school-computing", "Bachelor", "2026-2027", "Spring", "English", 5, List.of("Aruzhan Seitova", "Arman Idrisov"), List.of("cybersecurity", "risk management", "information security")),
                course("eco-214", "Macroeconomic Strategy", "ECO 214", "Economics", "school-business", "Bachelor", "2026-2027", "Autumn", "English", 5, List.of("Madina Akhmetova", "Askar Dulatuly"), List.of("macroeconomics", "economics", "strategy")),
                course("bus-415", "Strategic Operations Management", "BUS 415", "Business Administration", "school-business", "Bachelor", "2026-2027", "Summer", "Kazakh", 5, List.of("Alina Saparova", "Madina Akhmetova"), List.of("operations", "business strategy", "management")),
                course("fin-330", "Financial Analytics and Modeling", "FIN 330", "Finance", "school-business", "Bachelor", "2026-2027", "Spring", "English", 5, List.of("Askar Dulatuly", "Saltanat Orynbayeva"), List.of("finance", "analytics", "financial modeling")),
                course("ent-410", "Entrepreneurial Strategy Lab", "ENT 410", "Entrepreneurship", "school-business", "Master", "2026-2027", "Summer", "English", 4, List.of("Rauan Zhumabek", "Madina Akhmetova"), List.of("entrepreneurship", "innovation", "venture strategy")),
                course("eng-410", "Renewable Energy Systems", "ENG 410", "Energy Engineering", "school-engineering", "Master", "2026-2027", "Autumn", "English", 6, List.of("Aida Kerimbek", "Yernar Balgabayev"), List.of("renewable energy", "energy systems", "sustainability")),
                course("civ-322", "Civil Infrastructure Design", "CIV 322", "Civil Engineering", "school-engineering", "Bachelor", "2026-2027", "Spring", "Kazakh", 5, List.of("Bekzat Omar", "Yernar Balgabayev"), List.of("civil engineering", "infrastructure", "design")),
                course("aut-360", "Industrial Automation and Control", "AUT 360", "Electrical Engineering", "school-engineering", "Bachelor", "2026-2027", "Spring", "Russian", 5, List.of("Miras Akhmetzhan", "Aida Kerimbek"), List.of("automation", "control systems", "industrial engineering")),
                course("epi-340", "Epidemiology and Biostatistics", "EPI 340", "Public Health", "school-health", "Master", "2026-2027", "Autumn", "English", 6, List.of("Adil Beknazar", "Ainur Zhaksylyk"), List.of("epidemiology", "biostatistics", "public health")),
                course("phr-415", "Clinical Pharmacology", "PHR 415", "Pharmacy", "school-health", "Bachelor", "2026-2027", "Spring", "Russian", 5, List.of("Kamila Serikova", "Ainur Zhaksylyk"), List.of("pharmacology", "clinical practice", "medicines")),
                course("hin-305", "Health Informatics", "HIN 305", "Health Informatics", "school-health", "Bachelor", "2026-2027", "Summer", "English", 4, List.of("Moldir Yeleussiz", "Aizhan Kurmangali"), List.of("health informatics", "digital health", "medical data")),
                course("edu-601", "Inclusive Curriculum Design", "EDU 601", "Education Leadership", "school-education", "Master", "2026-2027", "Autumn", "Russian", 4, List.of("Aizada Bekenova", "Gulmira Tokayeva"), List.of("curriculum", "inclusive education", "pedagogy")),
                course("psy-410", "Educational Psychology in Practice", "PSY 410", "Educational Psychology", "school-education", "Bachelor", "2026-2027", "Spring", "Kazakh", 5, List.of("Nazym Rakhimova", "Aizada Bekenova"), List.of("educational psychology", "learning sciences", "student development")),
                course("lin-350", "Applied Linguistics for Educators", "LIN 350", "Linguistics in Education", "school-education", "Bachelor", "2026-2027", "Autumn", "English", 5, List.of("Dias Nurbek", "Gulmira Tokayeva"), List.of("linguistics", "language teaching", "education"))
        ).forEach(repository::save);
    }

    private void seedMegaProCache(CourseRepository courseRepository, MegaProResourceCacheRepository repository) {
        var courses = courseRepository.findAll().stream()
                .collect(java.util.stream.Collectors.toMap(CourseEntity::getId, course -> course, (left, right) -> left));
        var syncedAt = Instant.now();
        seedBooks().forEach((courseId, books) -> {
            var course = courses.get(courseId);
            if (course == null) {
                return;
            }
            books.forEach(book -> {
                var entity = repository.findByExternalId(book.externalId()).orElseGet(MegaProResourceCacheEntity::new);
                if (entity.getId() == null) {
                    entity.setId("megapro-cache-" + UUID.randomUUID());
                }
                entity.setCourseId(course.getId());
                entity.setDiscipline(course.getTitle());
                entity.setDisciplineTagsCsv(course.getDisciplineTagsCsv());
                entity.setExternalId(book.externalId());
                entity.setTitle(book.title());
                entity.setAuthor(book.author());
                entity.setPublicationYear(book.year());
                entity.setUrl(book.url());
                entity.setType(book.type());
                entity.setSyncedAt(syncedAt);
                repository.save(entity);
            });
        });
    }

    private Map<String, List<SeedBook>> seedBooks() {
        var publicPolicyKz = "\u041c\u0435\u043c\u043b\u0435\u043a\u0435\u0442\u0442\u0456\u043a \u0441\u0430\u044f\u0441\u0430\u0442\u0442\u044b \u0442\u0430\u043b\u0434\u0430\u0443";
        var macroeconomicsRu = "\u041c\u0430\u043a\u0440\u043e\u044d\u043a\u043e\u043d\u043e\u043c\u0438\u043a\u0430";

        return Map.of(
                "syllabus-public-policy-2026", List.of(
                        new SeedBook("mp-policy-001", "Public Policy Analysis", "William N. Dunn", "2024", "https://megapro.local/public-policy-analysis", "Textbook"),
                        new SeedBook("mp-policy-kz-001", publicPolicyKz, "Test Author", "2023", "https://megapro.local/memlekettik-sayasatty-taldau", "Textbook")
                ),
                "eco-214", List.of(
                        new SeedBook("mp-eco-001", "Macroeconomics", "N. Gregory Mankiw", "2024", "https://megapro.local/macroeconomics", "Textbook"),
                        new SeedBook("mp-eco-ru-001", macroeconomicsRu, "N. Gregory Mankiw", "2021", "https://megapro.local/makroekonomika", "Textbook")
                )
        );
    }

    private SchoolEntity school(String id, String code, String name, String directorUsername) {
        var entity = new SchoolEntity();
        entity.setId(id);
        entity.setCode(code);
        entity.setName(name);
        entity.setDirectorUsername(directorUsername);
        return entity;
    }

    private StaffProfileEntity staff(String id, String username, String fullName, String email, String workplace,
                                     String cabinet, String positionTitle, String schoolId, StaffRole role) {
        var entity = new StaffProfileEntity();
        entity.setId(id);
        entity.setUsername(username);
        entity.setFullName(fullName);
        entity.setEmail(email);
        entity.setWorkplace(workplace);
        entity.setCabinet(cabinet);
        entity.setPositionTitle(positionTitle);
        entity.setSchoolId(schoolId);
        entity.setRole(role);
        return entity;
    }

    private void upsertStaff(StaffProfileRepository repository, StaffProfileEntity seed) {
        var entity = repository.findByUsername(seed.getUsername()).orElseGet(StaffProfileEntity::new);
        entity.setId(seed.getId());
        entity.setUsername(seed.getUsername());
        entity.setFullName(seed.getFullName());
        entity.setEmail(seed.getEmail());
        entity.setWorkplace(seed.getWorkplace());
        entity.setCabinet(seed.getCabinet());
        entity.setPositionTitle(seed.getPositionTitle());
        entity.setSchoolId(seed.getSchoolId());
        entity.setRole(seed.getRole());
        repository.save(entity);
    }

    private CourseEntity course(String id, String title, String code, String program, String schoolId, String degreeLevel,
                                String academicYear, String trimester, String language, int credits,
                                List<String> instructors, List<String> tags) {
        var entity = new CourseEntity();
        entity.setId(id);
        entity.setTitle(title);
        entity.setCode(code);
        entity.setProgram(program);
        entity.setSchoolId(schoolId);
        entity.setDegreeLevel(degreeLevel);
        entity.setAcademicYear(academicYear);
        entity.setTrimester(trimester);
        entity.setLanguageOfInstruction(language);
        entity.setCredits(credits);
        entity.setInstructorsCsv(String.join("|", instructors));
        entity.setDisciplineTagsCsv(CourseMetadataSupport.toCsv(tags));
        return entity;
    }

    private record SeedBook(
            String externalId,
            String title,
            String author,
            String year,
            String url,
            String type
    ) {
    }
}
