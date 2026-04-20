package kz.iqadam.esyllabus.syllabus.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SyllabusMetricsCalculatorTests {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SyllabusContentFactory contentFactory = new SyllabusContentFactory(objectMapper);
    private final SyllabusMetricsCalculator calculator = new SyllabusMetricsCalculator();

    @Test
    void blankSyllabusHasLowCompletion() {
        var blank = contentFactory.createBlank();
        var metrics = calculator.calculate(blank);

        assertThat(metrics.sectionsTotal()).isEqualTo(17);
        assertThat(metrics.sectionsCompleted()).isEqualTo(0);
        assertThat(metrics.progress()).isEqualTo(0);
    }

    @Test
    void completeSyllabusGetsFullProgress() {
        var content = contentFactory.createBlank();
        content.put("title", "Test");
        content.put("code", "C-1");
        content.put("degreeLevel", "Bachelor");
        content.put("program", "CS");
        content.put("academicYear", "2026-2027");
        content.put("trimester", "Spring");
        content.put("languageOfInstruction", "English");
        content.put("credits", 5);
        content.put("prerequisites", "none");
        content.put("postrequisites", "none");
        content.put("overview", "<p>Overview</p>");
        content.put("academicIntegrity", "<p>Integrity</p>");
        content.put("inclusionStatements", "<p>Inclusion</p>");

        var workload = content.with("workload");
        workload.put("lecturesHours", 10);
        workload.put("practiceHours", 10);
        workload.put("labHours", 10);
        workload.put("iassHours", 10);
        workload.put("sisHours", 10);
        workload.put("totalHours", 50);

        content.withArray("instructors").add(objectMapper.createObjectNode()
                .put("fullName", "Teacher")
                .put("position", "Prof")
                .put("email", "teacher@uni.edu"));
        content.withArray("goals").add("Goal");
        content.withArray("learningOutcomes").add("Outcome");
        content.withArray("teachingMethods").add("Method");
        content.withArray("technologyEmployed").add("Moodle");
        content.withArray("gradingSchema").add(objectMapper.createObjectNode().put("name", "Exam").put("weight", 100));
        content.withArray("resources").add(objectMapper.createObjectNode().put("title", "Book"));
        content.withArray("abbreviations").add(objectMapper.createObjectNode().put("shortForm", "LMS").put("meaning", "Learning Management System"));
        content.withArray("weeklyPlan").add(objectMapper.createObjectNode().put("topic", "Week topic"));
        content.withArray("detailedPlan").add(objectMapper.createObjectNode().put("lectureTopics", "Lecture topic"));

        var policies = content.with("coursePolicies");
        policies.put("attendance", "Required");
        policies.put("lateSubmissions", "Allowed with penalty");
        policies.put("examsAttestation", "Midterm");
        policies.put("classroomBehavior", "Respect");
        policies.put("communicationPolicy", "Email");
        policies.put("otherPolicies", "None");

        var metrics = calculator.calculate(content);
        assertThat(metrics.sectionsCompleted()).isEqualTo(17);
        assertThat(metrics.sectionsTotal()).isEqualTo(17);
        assertThat(metrics.progress()).isEqualTo(100);
        assertThat(metrics.readyForReview()).isTrue();
    }
}
