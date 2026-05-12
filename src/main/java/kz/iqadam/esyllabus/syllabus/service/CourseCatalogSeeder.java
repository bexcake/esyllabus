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
        if (courseRepository.count() > 0) {
            courseRepository.findAll().forEach(this::backfillMetadataIfNeeded);
            return;
        }

        courseRepository.saveAll(List.of(
                createCourse("syllabus-public-policy-2026", "Public Policy Analysis and Design", "PPA 302", "Public Administration and Policy", "school-public-policy", "Bachelor", "2026-2027", "Spring", "English", 6, List.of("Dr. Aigerim Sadykova", "Marat Tulegenov"), List.of("public policy", "policy analysis", "governance")),
                createCourse("eco-214", "Macroeconomic Strategy", "ECO 214", "Economics", "school-business", "Bachelor", "2026-2027", "Autumn", "English", 5, List.of("Dana Utegenova"), List.of("macroeconomics", "economics", "strategy")),
                createCourse("cs-540", "Applied Machine Learning Studio", "CS 540", "Computer Science", "school-computing", "Master", "2026-2027", "Spring", "English", 7, List.of("Arman Idrisov", "Leila Baimurat"), List.of("machine learning", "artificial intelligence", "data science")),
                createCourse("bus-415", "Strategic Operations Management", "BUS 415", "Business Administration", "school-business", "Bachelor", "2026-2027", "Summer", "Kazakh", 5, List.of("Madina Akhmetova"), List.of("operations", "business strategy", "management")),
                createCourse("edu-601", "Inclusive Curriculum Design", "EDU 601", "Education Leadership", "school-public-policy", "Master", "2026-2027", "Autumn", "Russian", 4, List.of("Aizada Bekenova"), List.of("curriculum", "inclusive education", "pedagogy")),
                createCourse("law-331", "Comparative Constitutional Law", "LAW 331", "Law", "school-public-policy", "Bachelor", "2026-2027", "Spring", "English", 6, List.of("Timur Kenzhebayev"), List.of("constitutional law", "comparative law", "legal systems"))
        ));
    }

    private CourseEntity createCourse(
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
        var course = new CourseEntity();
        course.setId(id);
        course.setTitle(title);
        course.setCode(code);
        course.setProgram(program);
        course.setSchoolId(schoolId);
        course.setDegreeLevel(degreeLevel);
        course.setAcademicYear(academicYear);
        course.setTrimester(trimester);
        course.setLanguageOfInstruction(languageOfInstruction);
        course.setCredits(credits);
        course.setInstructorsCsv(String.join("|", instructors));
        course.setDisciplineTagsCsv(CourseMetadataSupport.toCsv(disciplineTags));
        return course;
    }

    private void backfillMetadataIfNeeded(CourseEntity course) {
        if (course.getSchoolId() != null && !course.getSchoolId().isBlank()
                && course.getDisciplineTagsCsv() != null && !course.getDisciplineTagsCsv().isBlank()) {
            return;
        }

        switch (course.getId()) {
            case "syllabus-public-policy-2026" -> {
                course.setSchoolId("school-public-policy");
                course.setDisciplineTagsCsv(CourseMetadataSupport.toCsv(List.of("public policy", "policy analysis", "governance")));
            }
            case "eco-214" -> {
                course.setSchoolId("school-business");
                course.setDisciplineTagsCsv(CourseMetadataSupport.toCsv(List.of("macroeconomics", "economics", "strategy")));
            }
            case "cs-540" -> {
                course.setSchoolId("school-computing");
                course.setDisciplineTagsCsv(CourseMetadataSupport.toCsv(List.of("machine learning", "artificial intelligence", "data science")));
            }
            case "bus-415" -> {
                course.setSchoolId("school-business");
                course.setDisciplineTagsCsv(CourseMetadataSupport.toCsv(List.of("operations", "business strategy", "management")));
            }
            case "edu-601" -> {
                course.setSchoolId("school-public-policy");
                course.setDisciplineTagsCsv(CourseMetadataSupport.toCsv(List.of("curriculum", "inclusive education", "pedagogy")));
            }
            case "law-331" -> {
                course.setSchoolId("school-public-policy");
                course.setDisciplineTagsCsv(CourseMetadataSupport.toCsv(List.of("constitutional law", "comparative law", "legal systems")));
            }
            default -> {
                course.setSchoolId(course.getSchoolId() == null ? "school-public-policy" : course.getSchoolId());
                if (course.getDisciplineTagsCsv() == null || course.getDisciplineTagsCsv().isBlank()) {
                    course.setDisciplineTagsCsv(CourseMetadataSupport.toCsv(
                            CourseMetadataSupport.defaultTags(course.getTitle(), course.getProgram(), course.getCode())
                    ));
                }
            }
        }

        courseRepository.save(course);
    }
}
