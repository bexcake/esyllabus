package kz.iqadam.esyllabus.syllabus.service;

import java.util.List;
import kz.iqadam.esyllabus.syllabus.persistence.CourseEntity;
import kz.iqadam.esyllabus.syllabus.persistence.CourseRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(CourseRepository.class)
public class CourseCatalogSeeder implements CommandLineRunner {

    private final CourseRepository courseRepository;

    public CourseCatalogSeeder(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    @Override
    public void run(String... args) {
        if (courseRepository.count() > 0) {
            return;
        }

        courseRepository.saveAll(List.of(
                createCourse("syllabus-public-policy-2026", "Public Policy Analysis and Design", "PPA 302", "Public Administration and Policy", "Bachelor", "2026-2027", "Spring", "English", 6, List.of("Dr. Aigerim Sadykova", "Marat Tulegenov")),
                createCourse("eco-214", "Macroeconomic Strategy", "ECO 214", "Economics", "Bachelor", "2026-2027", "Autumn", "English", 5, List.of("Dana Utegenova")),
                createCourse("cs-540", "Applied Machine Learning Studio", "CS 540", "Computer Science", "Master", "2026-2027", "Spring", "English", 7, List.of("Arman Idrisov", "Leila Baimurat")),
                createCourse("bus-415", "Strategic Operations Management", "BUS 415", "Business Administration", "Bachelor", "2026-2027", "Summer", "Kazakh", 5, List.of("Madina Akhmetova")),
                createCourse("edu-601", "Inclusive Curriculum Design", "EDU 601", "Education Leadership", "Master", "2026-2027", "Autumn", "Russian", 4, List.of("Aizada Bekenova")),
                createCourse("law-331", "Comparative Constitutional Law", "LAW 331", "Law", "Bachelor", "2026-2027", "Spring", "English", 6, List.of("Timur Kenzhebayev"))
        ));
    }

    private CourseEntity createCourse(
            String id,
            String title,
            String code,
            String program,
            String degreeLevel,
            String academicYear,
            String trimester,
            String languageOfInstruction,
            int credits,
            List<String> instructors
    ) {
        var course = new CourseEntity();
        course.setId(id);
        course.setTitle(title);
        course.setCode(code);
        course.setProgram(program);
        course.setDegreeLevel(degreeLevel);
        course.setAcademicYear(academicYear);
        course.setTrimester(trimester);
        course.setLanguageOfInstruction(languageOfInstruction);
        course.setCredits(credits);
        course.setInstructorsCsv(String.join("|", instructors));
        return course;
    }
}
