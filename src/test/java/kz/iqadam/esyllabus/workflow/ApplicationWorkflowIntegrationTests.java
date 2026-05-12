package kz.iqadam.esyllabus.workflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class ApplicationWorkflowIntegrationTests {

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    void exposesDirectoryCatalogs() throws Exception {
        mockMvc.perform(get("/api/directory/students")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("teacher", "teacher123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").exists())
                .andExpect(jsonPath("$[0].groupName").exists())
                .andExpect(jsonPath("$[0].currentCourses[0].id").exists());

        mockMvc.perform(get("/api/directory/staff")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("teacher", "teacher123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].schoolName").exists())
                .andExpect(jsonPath("$[0].cabinet").exists())
                .andExpect(jsonPath("$[0].role").value("TEACHER"));

        mockMvc.perform(get("/api/directory/students/me")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("student", "student123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("student"))
                .andExpect(jsonPath("$.currentCourses[0].id").exists());
    }

    @Test
    void supportsLibraryRequestWorkflowFromTeacherToDirectorToLibrarian() throws Exception {
        var createPayload = Map.of(
                "department", "School of Public Policy",
                "educationLevel", "Bachelor",
                "items", List.of(Map.ofEntries(
                        Map.entry("title", "Policy Design Handbook"),
                        Map.entry("author", "A. Author"),
                        Map.entry("isbn", "978-1-23-456789-0"),
                        Map.entry("publisher", "IQadam Press"),
                        Map.entry("publicationYear", "2025"),
                        Map.entry("discipline", "Public Policy Analysis and Design"),
                        Map.entry("educationalProgram", "Public Administration and Policy"),
                        Map.entry("courseNumber", 2),
                        Map.entry("trimester", "Spring"),
                        Map.entry("quantity", 3),
                        Map.entry("literatureType", "Textbook")
                ))
        );

        var createResponse = mockMvc.perform(post("/api/library/requests")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("teacher", "teacher123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Draft"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String requestId = objectMapper.readTree(createResponse).path("id").asText();
        assertThat(requestId).isNotBlank();

        mockMvc.perform(get("/api/library/requests")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("librarian", "librarian123")))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        mockMvc.perform(post("/api/library/requests/{requestId}/submit", requestId)
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("teacher", "teacher123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Pending Director Approval"));

        mockMvc.perform(post("/api/library/requests/{requestId}/director-approve", requestId)
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("director", "director123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Approved by Director"));

        mockMvc.perform(get("/api/library/requests")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("librarian", "librarian123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(requestId));

        mockMvc.perform(post("/api/library/requests/{requestId}/library-feedback", requestId)
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("librarian", "librarian123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "feedback", "Planned for procurement in the summer batch",
                                "expectedPurchaseMonth", "2026-08"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Feedback Provided"))
                .andExpect(jsonPath("$.expectedPurchaseMonth").value("2026-08"));

        var exportResponse = mockMvc.perform(get("/api/library/requests/{requestId}/export-form", requestId)
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("librarian", "librarian123")))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andReturn()
                .getResponse();

        assertThat(exportResponse.getContentAsByteArray()).isNotEmpty();
    }

    @Test
    void publishesSyllabusAfterColleagueAndDirectorApprovalAndShowsItToStudent() throws Exception {
        mockMvc.perform(get("/api/courses")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("student", "student123")))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        var syllabusResponse = mockMvc.perform(post("/api/syllabi")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("teacher", "teacher123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("courseId", "eco-214"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode createdSyllabus = objectMapper.readTree(syllabusResponse);
        String syllabusId = createdSyllabus.path("id").asText();
        assertThat(syllabusId).isNotBlank();

        mockMvc.perform(put("/api/syllabi/{syllabusId}/reviewers", syllabusId)
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("teacher", "teacher123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reviewerUsernames", List.of("director")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.colleagueApprovals[0].username").value("director"))
                .andExpect(jsonPath("$.colleagueApprovals[0].approved").value(false));

        var completedContent = buildCompleteSyllabusContent((ObjectNode) createdSyllabus.path("content").deepCopy(), "Modern Macroeconomics");

        mockMvc.perform(put("/api/syllabi/{syllabusId}", syllabusId)
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("teacher", "teacher123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completedContent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progress").value(100));

        mockMvc.perform(post("/api/syllabi/{syllabusId}/submit-review", syllabusId)
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("teacher", "teacher123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Pending Colleague Confirmation"));

        mockMvc.perform(get("/api/syllabi/review-queue")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("director", "director123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(syllabusId))
                .andExpect(jsonPath("$[0].colleagueApprovals[0].approved").value(false));

        mockMvc.perform(post("/api/syllabi/{syllabusId}/colleague-approve", syllabusId)
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("director", "director123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Pending Director Review"))
                .andExpect(jsonPath("$.colleagueApprovals[0].approved").value(true));

        var approvedResponse = mockMvc.perform(post("/api/syllabi/{syllabusId}/approve", syllabusId)
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("director", "director123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Published"))
                .andExpect(jsonPath("$.linkedLibraryRequestId").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String linkedLibraryRequestId = objectMapper.readTree(approvedResponse).path("linkedLibraryRequestId").asText();
        assertThat(linkedLibraryRequestId).isNotBlank();

        mockMvc.perform(get("/api/courses")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("student", "student123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("eco-214"))
                .andExpect(jsonPath("$[0].status").value("Published"))
                .andExpect(jsonPath("$[0].syllabusId").value(syllabusId));

        mockMvc.perform(get("/api/syllabi/{syllabusId}", syllabusId)
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("student", "student123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(syllabusId))
                .andExpect(jsonPath("$.status").value("Published"));

        mockMvc.perform(get("/api/library/requests")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("librarian", "librarian123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(linkedLibraryRequestId))
                .andExpect(jsonPath("$[0].syllabusId").value(syllabusId));
    }

    @Test
    void exposesDisciplineTagsAndPdfExport() throws Exception {
        mockMvc.perform(get("/api/courses")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("teacher", "teacher123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].disciplineTags").isArray())
                .andExpect(jsonPath("$[0].schoolId").exists());

        mockMvc.perform(get("/api/library/disciplines")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("teacher", "teacher123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].disciplineTags").isArray())
                .andExpect(jsonPath("$[0].courseId").exists());

        mockMvc.perform(get("/api/library/books")
                        .param("query", "macro")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("teacher", "teacher123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").exists())
                .andExpect(jsonPath("$[0].discipline").exists());

        var syllabusResponse = mockMvc.perform(post("/api/syllabi")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("teacher", "teacher123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String syllabusId = objectMapper.readTree(syllabusResponse).path("id").asText();
        var pdfResponse = mockMvc.perform(get("/api/syllabi/{syllabusId}/export-pdf", syllabusId)
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("teacher", "teacher123")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn()
                .getResponse();

        assertThat(new String(pdfResponse.getContentAsByteArray(), 0, 4)).isEqualTo("%PDF");
    }

    private ObjectNode buildCompleteSyllabusContent(ObjectNode content, String resourceTitle) {
        content.put("title", "Macroeconomic Strategy");
        content.put("code", "ECO 214");
        content.put("degreeLevel", "Bachelor");
        content.put("program", "Economics");
        content.put("academicYear", "2026-2027");
        content.put("trimester", "Autumn");
        content.put("languageOfInstruction", "English");
        content.put("credits", 5);
        content.put("prerequisites", "Introduction to economics");
        content.put("postrequisites", "Advanced economic policy");
        content.put("overview", "This course covers macroeconomic strategy and policy design.");
        content.put("academicIntegrity", "Academic honesty is mandatory.");
        content.put("inclusionStatements", "Reasonable accommodations are provided.");

        var workload = content.with("workload");
        workload.put("lecturesHours", 15);
        workload.put("practiceHours", 15);
        workload.put("labHours", 0);
        workload.put("iassHours", 30);
        workload.put("sisHours", 60);
        workload.put("totalHours", 120);

        content.set("instructors", instructorsArray());
        content.set("goals", stringArray("Understand macroeconomic tools"));
        content.set("learningOutcomes", stringArray("Evaluate macroeconomic policy choices"));
        content.set("teachingMethods", stringArray("Case discussion"));
        content.set("technologyEmployed", stringArray("Moodle"));

        var gradingSchema = objectMapper.createArrayNode();
        gradingSchema.addObject()
                .put("name", "Midterm")
                .put("weight", 40);
        gradingSchema.addObject()
                .put("name", "Final")
                .put("weight", 60);
        content.set("gradingSchema", gradingSchema);

        var policies = content.with("coursePolicies");
        policies.put("attendance", "Attendance is required.");
        policies.put("lateSubmissions", "Late work loses points.");
        policies.put("examsAttestation", "Exam rules follow university policy.");
        policies.put("classroomBehavior", "Respectful behavior is expected.");
        policies.put("communicationPolicy", "Email responses within two business days.");
        policies.put("otherPolicies", "Additional policies may be announced.");

        var resources = objectMapper.createArrayNode();
        resources.addObject()
                .put("title", resourceTitle)
                .put("author", "John Economist")
                .put("year", "2024")
                .put("type", "Textbook")
                .put("publisher", "Global Economics Press")
                .put("isbn", "978-1-4028-9462-6")
                .put("quantity", 2)
                .put("isRequired", true);
        content.set("resources", resources);

        var abbreviations = objectMapper.createArrayNode();
        abbreviations.addObject()
                .put("shortForm", "GDP")
                .put("meaning", "Gross Domestic Product");
        content.set("abbreviations", abbreviations);

        var weeklyPlan = objectMapper.createArrayNode();
        weeklyPlan.addObject()
                .put("topic", "Macroeconomic indicators");
        content.set("weeklyPlan", weeklyPlan);

        var detailedPlan = objectMapper.createArrayNode();
        detailedPlan.addObject()
                .put("lectureTopics", "Inflation and monetary policy");
        content.set("detailedPlan", detailedPlan);

        content.set("optionalSections", objectMapper.createArrayNode());
        content.set("customSections", objectMapper.createArrayNode());
        return content;
    }

    private ArrayNode instructorsArray() {
        var instructors = objectMapper.createArrayNode();
        instructors.addObject()
                .put("id", "instructor-1")
                .put("fullName", "Aigerim Sadykova")
                .put("position", "Senior Teacher")
                .put("email", "a.sadykova@iqadam.kz")
                .put("officeOrContact", "B-204")
                .put("isPrimary", true);
        return instructors;
    }

    private ArrayNode stringArray(String value) {
        var array = objectMapper.createArrayNode();
        array.add(value);
        return array;
    }
}
