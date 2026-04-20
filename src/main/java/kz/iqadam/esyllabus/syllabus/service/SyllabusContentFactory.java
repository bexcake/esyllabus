package kz.iqadam.esyllabus.syllabus.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.UUID;
import kz.iqadam.esyllabus.syllabus.persistence.CourseEntity;
import org.springframework.stereotype.Component;

@Component
public class SyllabusContentFactory {

    private final ObjectMapper objectMapper;

    public SyllabusContentFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ObjectNode createFromCourse(CourseEntity course) {
        var syllabus = createBlank();
        syllabus.put("title", course.getTitle());
        syllabus.put("code", course.getCode());
        syllabus.put("degreeLevel", course.getDegreeLevel());
        syllabus.put("program", course.getProgram());
        syllabus.put("academicYear", course.getAcademicYear());
        syllabus.put("trimester", course.getTrimester());
        syllabus.put("languageOfInstruction", course.getLanguageOfInstruction());
        syllabus.put("credits", course.getCredits());
        syllabus.set("instructors", createInstructorsFromCourse(course.getInstructorsCsv()));
        return syllabus;
    }

    public ObjectNode createBlank() {
        var syllabus = objectMapper.createObjectNode();
        syllabus.put("id", UUID.randomUUID().toString());
        syllabus.put("title", "");
        syllabus.put("code", "");
        syllabus.put("degreeLevel", "");
        syllabus.put("program", "");
        syllabus.put("academicYear", "");
        syllabus.put("trimester", "");
        syllabus.put("languageOfInstruction", "");
        syllabus.put("credits", 0);

        var workload = objectMapper.createObjectNode();
        workload.put("lecturesHours", 0);
        workload.put("practiceHours", 0);
        workload.put("labHours", 0);
        workload.put("iassHours", 0);
        workload.put("sisHours", 0);
        workload.put("totalHours", 0);
        syllabus.set("workload", workload);

        syllabus.put("prerequisites", "");
        syllabus.put("postrequisites", "");
        syllabus.set("instructors", objectMapper.createArrayNode());
        syllabus.put("overview", "");
        syllabus.set("goals", objectMapper.createArrayNode());
        syllabus.set("learningOutcomes", objectMapper.createArrayNode());
        syllabus.set("teachingMethods", objectMapper.createArrayNode());
        syllabus.set("gradingSchema", objectMapper.createArrayNode());
        syllabus.put("academicIntegrity", "");

        var coursePolicies = objectMapper.createObjectNode();
        coursePolicies.put("attendance", "");
        coursePolicies.put("lateSubmissions", "");
        coursePolicies.put("examsAttestation", "");
        coursePolicies.put("classroomBehavior", "");
        coursePolicies.put("communicationPolicy", "");
        coursePolicies.put("otherPolicies", "");
        syllabus.set("coursePolicies", coursePolicies);

        syllabus.put("inclusionStatements", "");
        syllabus.set("technologyEmployed", objectMapper.createArrayNode());
        syllabus.set("resources", objectMapper.createArrayNode());
        syllabus.set("abbreviations", objectMapper.createArrayNode());
        syllabus.set("weeklyPlan", objectMapper.createArrayNode());
        syllabus.set("detailedPlan", objectMapper.createArrayNode());
        syllabus.set("optionalSections", objectMapper.createArrayNode());
        syllabus.set("customSections", objectMapper.createArrayNode());
        return syllabus;
    }

    public ArrayNode appendResources(ArrayNode resources, java.util.List<LibraryResourceSeed> books) {
        for (var book : books) {
            var resource = objectMapper.createObjectNode();
            resource.put("id", UUID.randomUUID().toString());
            resource.put("title", book.title());
            resource.put("author", book.author());
            resource.put("year", book.year());
            resource.put("type", book.type());
            resource.put("url", book.url());
            resource.put("isRequired", false);
            resource.put("notes", "Imported from MegaPro");
            resources.add(resource);
        }
        return resources;
    }

    public record LibraryResourceSeed(
            String title,
            String author,
            String year,
            String type,
            String url
    ) {
    }

    private ArrayNode createInstructorsFromCourse(String instructorsCsv) {
        var result = objectMapper.createArrayNode();
        var instructors = parseInstructors(instructorsCsv);
        for (int index = 0; index < instructors.size(); index++) {
            var instructor = objectMapper.createObjectNode();
            instructor.put("id", UUID.randomUUID().toString());
            instructor.put("fullName", instructors.get(index));
            instructor.put("position", "");
            instructor.put("email", "");
            instructor.put("officeOrContact", "");
            instructor.put("isPrimary", index == 0);
            result.add(instructor);
        }
        return result;
    }

    static List<String> parseInstructors(String instructorsCsv) {
        if (instructorsCsv == null || instructorsCsv.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(instructorsCsv.split("\\|"))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }
}
