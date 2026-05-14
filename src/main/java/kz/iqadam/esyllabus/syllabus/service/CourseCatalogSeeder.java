package kz.iqadam.esyllabus.syllabus.service;

import java.util.List;
import kz.iqadam.esyllabus.syllabus.persistence.CourseEntity;
import kz.iqadam.esyllabus.syllabus.persistence.CourseRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(10)
public class CourseCatalogSeeder implements CommandLineRunner {

    private final CourseRepository courseRepository;

    public CourseCatalogSeeder(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        for (var seed : seeds()) {
            upsertCourse(seed);
        }

        courseRepository.findAll().forEach(this::backfillMetadataIfNeeded);
    }

    private List<CourseSeed> seeds() {
        return List.of(
                new CourseSeed("syllabus-public-policy-2026", "Public Policy Analysis and Design", "PPA 302", "Public Administration and Policy", "school-public-policy", "Bachelor", "2026-2027", "Spring", "English", 6, List.of("Aigerim Sadykova", "Marat Tulegenov"), List.of("public policy", "policy analysis", "governance")),
                new CourseSeed("law-331", "Comparative Constitutional Law", "LAW 331", "Law", "school-public-policy", "Bachelor", "2026-2027", "Spring", "English", 6, List.of("Timur Kenzhebayev", "Zarina Mukasheva"), List.of("constitutional law", "comparative law", "legal systems")),
                new CourseSeed("ppg-415", "Sustainable Urban Governance", "PPG 415", "Public Governance", "school-public-policy", "Master", "2026-2027", "Autumn", "English", 5, List.of("Dana Utegenova", "Aigerim Sadykova"), List.of("urban governance", "sustainability", "public management")),

                new CourseSeed("cs-540", "Applied Machine Learning Studio", "CS 540", "Computer Science", "school-computing", "Master", "2026-2027", "Spring", "English", 7, List.of("Arman Idrisov", "Leila Baimurat"), List.of("machine learning", "artificial intelligence", "data science")),
                new CourseSeed("cs-622", "Cloud Systems Engineering", "CS 622", "Software Engineering", "school-computing", "Master", "2026-2027", "Autumn", "English", 6, List.of("Daniyar Sarsembayev", "Leila Baimurat"), List.of("cloud computing", "distributed systems", "software engineering")),
                new CourseSeed("cs-575", "Cybersecurity Governance", "CS 575", "Cybersecurity", "school-computing", "Bachelor", "2026-2027", "Spring", "English", 5, List.of("Aruzhan Seitova", "Arman Idrisov"), List.of("cybersecurity", "risk management", "information security")),

                new CourseSeed("eco-214", "Macroeconomic Strategy", "ECO 214", "Economics", "school-business", "Bachelor", "2026-2027", "Autumn", "English", 5, List.of("Madina Akhmetova", "Askar Dulatuly"), List.of("macroeconomics", "economics", "strategy")),
                new CourseSeed("bus-415", "Strategic Operations Management", "BUS 415", "Business Administration", "school-business", "Bachelor", "2026-2027", "Summer", "Kazakh", 5, List.of("Alina Saparova", "Madina Akhmetova"), List.of("operations", "business strategy", "management")),
                new CourseSeed("fin-330", "Financial Analytics and Modeling", "FIN 330", "Finance", "school-business", "Bachelor", "2026-2027", "Spring", "English", 5, List.of("Askar Dulatuly", "Saltanat Orynbayeva"), List.of("finance", "analytics", "financial modeling")),
                new CourseSeed("ent-410", "Entrepreneurial Strategy Lab", "ENT 410", "Entrepreneurship", "school-business", "Master", "2026-2027", "Summer", "English", 4, List.of("Rauan Zhumabek", "Madina Akhmetova"), List.of("entrepreneurship", "innovation", "venture strategy")),

                new CourseSeed("eng-410", "Renewable Energy Systems", "ENG 410", "Energy Engineering", "school-engineering", "Master", "2026-2027", "Autumn", "English", 6, List.of("Aida Kerimbek", "Yernar Balgabayev"), List.of("renewable energy", "energy systems", "sustainability")),
                new CourseSeed("civ-322", "Civil Infrastructure Design", "CIV 322", "Civil Engineering", "school-engineering", "Bachelor", "2026-2027", "Spring", "Kazakh", 5, List.of("Bekzat Omar", "Yernar Balgabayev"), List.of("civil engineering", "infrastructure", "design")),
                new CourseSeed("aut-360", "Industrial Automation and Control", "AUT 360", "Electrical Engineering", "school-engineering", "Bachelor", "2026-2027", "Spring", "Russian", 5, List.of("Miras Akhmetzhan", "Aida Kerimbek"), List.of("automation", "control systems", "industrial engineering")),

                new CourseSeed("epi-340", "Epidemiology and Biostatistics", "EPI 340", "Public Health", "school-health", "Master", "2026-2027", "Autumn", "English", 6, List.of("Adil Beknazar", "Ainur Zhaksylyk"), List.of("epidemiology", "biostatistics", "public health")),
                new CourseSeed("phr-415", "Clinical Pharmacology", "PHR 415", "Pharmacy", "school-health", "Bachelor", "2026-2027", "Spring", "Russian", 5, List.of("Kamila Serikova", "Ainur Zhaksylyk"), List.of("pharmacology", "clinical practice", "medicines")),
                new CourseSeed("hin-305", "Health Informatics", "HIN 305", "Health Informatics", "school-health", "Bachelor", "2026-2027", "Summer", "English", 4, List.of("Moldir Yeleussiz", "Aizhan Kurmangali"), List.of("health informatics", "digital health", "medical data")),

                new CourseSeed("edu-601", "Inclusive Curriculum Design", "EDU 601", "Education Leadership", "school-education", "Master", "2026-2027", "Autumn", "Russian", 4, List.of("Aizada Bekenova", "Gulmira Tokayeva"), List.of("curriculum", "inclusive education", "pedagogy")),
                new CourseSeed("psy-410", "Educational Psychology in Practice", "PSY 410", "Educational Psychology", "school-education", "Bachelor", "2026-2027", "Spring", "Kazakh", 5, List.of("Nazym Rakhimova", "Aizada Bekenova"), List.of("educational psychology", "learning sciences", "student development")),
                new CourseSeed("lin-350", "Applied Linguistics for Educators", "LIN 350", "Linguistics in Education", "school-education", "Bachelor", "2026-2027", "Autumn", "English", 5, List.of("Dias Nurbek", "Gulmira Tokayeva"), List.of("linguistics", "language teaching", "education"))
        );
    }

    private void upsertCourse(CourseSeed seed) {
        var entity = courseRepository.findById(seed.id()).orElseGet(CourseEntity::new);
        entity.setId(seed.id());
        entity.setTitle(seed.title());
        entity.setCode(seed.code());
        entity.setProgram(seed.program());
        entity.setSchoolId(seed.schoolId());
        entity.setDegreeLevel(seed.degreeLevel());
        entity.setAcademicYear(seed.academicYear());
        entity.setTrimester(seed.trimester());
        entity.setLanguageOfInstruction(seed.languageOfInstruction());
        entity.setCredits(seed.credits());
        entity.setInstructorsCsv(String.join("|", seed.instructors()));
        entity.setDisciplineTagsCsv(CourseMetadataSupport.toCsv(seed.disciplineTags()));
        courseRepository.save(entity);
    }

    private void backfillMetadataIfNeeded(CourseEntity course) {
        if (course.getSchoolId() != null && !course.getSchoolId().isBlank()
                && course.getDisciplineTagsCsv() != null && !course.getDisciplineTagsCsv().isBlank()) {
            return;
        }

        course.setSchoolId(course.getSchoolId() == null ? "school-public-policy" : course.getSchoolId());
        if (course.getDisciplineTagsCsv() == null || course.getDisciplineTagsCsv().isBlank()) {
            course.setDisciplineTagsCsv(CourseMetadataSupport.toCsv(
                    CourseMetadataSupport.defaultTags(course.getTitle(), course.getProgram(), course.getCode())
            ));
        }
        courseRepository.save(course);
    }

    private record CourseSeed(
            String id,
            String title,
            String code,
            String program,
            String schoolId,
            String degreeLevel,
            String academicYear,
            String trimester,
            String languageOfInstruction,
            int credits,
            List<String> instructors,
            List<String> disciplineTags
    ) {
    }
}
