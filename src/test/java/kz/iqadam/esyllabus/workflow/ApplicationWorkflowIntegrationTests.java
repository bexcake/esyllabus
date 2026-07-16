package kz.iqadam.esyllabus.workflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "digital-university.jwt.enabled=true",
        "digital-university.jwt.secret=dGVzdC1kdS1zZWNyZXQ="
})
@Import(WorkflowTestFixturesConfiguration.class)
class ApplicationWorkflowIntegrationTests {

    private static final String SECRET = "test-du-secret";

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
        mockMvc.perform(get("/api/directory/schools")
                        .with(duUser("teacher", "Aigerim Sadykova", "teacher")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[5].id").exists());

        mockMvc.perform(get("/api/directory/staff")
                        .with(duUser("teacher", "Aigerim Sadykova", "teacher")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].schoolName").exists())
                .andExpect(jsonPath("$[0].cabinet").exists())
                .andExpect(jsonPath("$[0].role").value("TEACHER"));

        mockMvc.perform(get("/api/directory/staff")
                        .param("schoolId", "school-computing")
                        .with(duUser("teacher", "Aigerim Sadykova", "teacher")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[2].username").exists())
                .andExpect(jsonPath("$[0].schoolId").value("school-computing"));

        mockMvc.perform(get("/api/directory/staff/teacher")
                        .with(duUser("teacher", "Aigerim Sadykova", "teacher")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("teacher"))
                .andExpect(jsonPath("$.schoolId").value("school-public-policy"));

        mockMvc.perform(get("/api/directory/staff/picker")
                        .param("schoolId", "school-public-policy")
                        .with(duUser("teacher", "Aigerim Sadykova", "teacher")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").exists())
                .andExpect(jsonPath("$[0].schoolId").value("school-public-policy"));

        mockMvc.perform(get("/api/directory/programs")
                        .with(duUser("teacher", "Aigerim Sadykova", "teacher")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[8].id").exists())
                .andExpect(jsonPath("$[0].code").exists())
                .andExpect(jsonPath("$[0].degreeLevel").exists());

        mockMvc.perform(get("/api/directory/academic-years")
                        .with(duUser("teacher", "Aigerim Sadykova", "teacher")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].value").exists());

        mockMvc.perform(get("/api/directory/trimesters")
                        .with(duUser("teacher", "Aigerim Sadykova", "teacher")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].value").exists());

        mockMvc.perform(get("/api/directory/languages")
                        .with(duUser("teacher", "Aigerim Sadykova", "teacher")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].value").exists());

        mockMvc.perform(get("/api/directory/degree-levels")
                        .with(duUser("teacher", "Aigerim Sadykova", "teacher")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].value").value("Bachelor"));

        mockMvc.perform(get("/api/directory/course-types")
                        .with(duUser("teacher", "Aigerim Sadykova", "teacher")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].value").value("Compulsory"));

        mockMvc.perform(get("/api/directory/assessment-stages")
                        .with(duUser("teacher", "Aigerim Sadykova", "teacher")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].value").value("Continuous assessment"));

        var syllabusResponse = mockMvc.perform(post("/api/syllabi")
                        .with(duUser("teacher", "Aigerim Sadykova", "teacher"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("courseId", "syllabus-public-policy-2026"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String syllabusId = objectMapper.readTree(syllabusResponse).path("id").asText();

        mockMvc.perform(get("/api/directory/reviewers")
                        .param("syllabusId", syllabusId)
                        .with(duUser("teacher", "Aigerim Sadykova", "teacher")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").exists())
                .andExpect(jsonPath("$[?(@.username == 'director')]").isEmpty());

        mockMvc.perform(get("/api/syllabi/{syllabusId}/metadata-options", syllabusId)
                        .with(duUser("teacher", "Aigerim Sadykova", "teacher")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowedInstructors[0].username").exists())
                .andExpect(jsonPath("$.allowedReviewers[0].username").exists())
                .andExpect(jsonPath("$.allowedReviewers[?(@.username == 'director')]").isEmpty())
                .andExpect(jsonPath("$.allowedDirectors[0].username").value("director"))
                .andExpect(jsonPath("$.allowedDirectors[?(@.username == 'director-business')]").isNotEmpty())
                .andExpect(jsonPath("$.allowedReviewers[?(@.username == 'teacher-business')]").isNotEmpty())
                .andExpect(jsonPath("$.programs[0].id").exists())
                .andExpect(jsonPath("$.academicYears[0].value").exists())
                .andExpect(jsonPath("$.assessmentStages[0].value").value("Continuous assessment"));
    }

    @Test
    void exposesDiverseDirectoryFixturesForFrontendCases() throws Exception {
        JsonNode schools = readJson(get("/api/directory/schools")
                .with(duUser("teacher", "Aigerim Sadykova", "teacher")));
        assertThat(schools.size()).isGreaterThanOrEqualTo(6);
        assertThat(collectTexts(schools, "id")).contains(
                "school-public-policy",
                "school-computing",
                "school-business",
                "school-engineering",
                "school-health",
                "school-education"
        );
        assertThat(collectTexts(schools, "directorUsername")).hasSizeGreaterThanOrEqualTo(6);
        for (JsonNode school : schools) {
            assertThat(school.path("staffCount").asInt()).isGreaterThanOrEqualTo(5);
        }

        JsonNode staff = readJson(get("/api/directory/staff")
                .with(duUser("teacher", "Aigerim Sadykova", "teacher")));
        assertThat(staff.size()).isGreaterThanOrEqualTo(30);
        assertThat(collectTexts(staff, "schoolId")).hasSizeGreaterThanOrEqualTo(6);
        assertThat(countByFieldValue(staff, "role", "SCHOOL_DIRECTOR")).isGreaterThanOrEqualTo(6);
        assertThat(collectTexts(staff, "username")).contains("teacher-business", "director-health", "librarian-sciences");

        JsonNode healthStaff = readJson(get("/api/directory/staff")
                .param("schoolId", "school-health")
                .with(duUser("teacher", "Aigerim Sadykova", "teacher")));
        assertThat(healthStaff.size()).isGreaterThanOrEqualTo(5);
        assertThat(collectTexts(healthStaff, "schoolId")).containsOnly("school-health");
        assertThat(countByFieldValue(healthStaff, "role", "SCHOOL_DIRECTOR")).isEqualTo(1);

        JsonNode computingPicker = readJson(get("/api/directory/staff/picker")
                .param("schoolId", "school-computing")
                .param("role", "TEACHER")
                .param("search", "cyber")
                .with(duUser("teacher", "Aigerim Sadykova", "teacher")));
        assertThat(computingPicker.size()).isEqualTo(1);
        assertThat(computingPicker.get(0).path("username").asText()).isEqualTo("teacher-cyber");

        JsonNode programs = readJson(get("/api/directory/programs")
                .with(duUser("teacher", "Aigerim Sadykova", "teacher")));
        assertThat(programs.size()).isGreaterThanOrEqualTo(18);
        assertThat(collectTexts(programs, "schoolId")).hasSizeGreaterThanOrEqualTo(6);
        assertThat(collectTexts(programs, "degreeLevel")).contains("Bachelor", "Master");
        assertThat(collectTexts(programs, "name")).contains("Computer Science", "Finance", "Public Health");

        JsonNode healthBachelorPrograms = readJson(get("/api/directory/programs")
                .param("schoolId", "school-health")
                .param("degreeLevel", "Bachelor")
                .with(duUser("teacher", "Aigerim Sadykova", "teacher")));
        assertThat(healthBachelorPrograms.size()).isGreaterThanOrEqualTo(2);
        assertThat(collectTexts(healthBachelorPrograms, "schoolId")).containsOnly("school-health");
        assertThat(collectTexts(healthBachelorPrograms, "degreeLevel")).containsOnly("Bachelor");
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
                        .with(duUser("teacher", "Aigerim Sadykova", "teacher"))
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
                        .with(duUser("librarian", "Library Specialist", "librarian")))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        mockMvc.perform(post("/api/library/requests/{requestId}/submit", requestId)
                        .with(duUser("teacher", "Aigerim Sadykova", "teacher")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Pending Director Approval"));

        mockMvc.perform(post("/api/library/requests/{requestId}/director-approve", requestId)
                        .with(duUser("director", "Public Policy Director", "director")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Approved by Director"));

        mockMvc.perform(get("/api/library/requests")
                        .with(duUser("librarian", "Library Specialist", "librarian")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(requestId));

        mockMvc.perform(post("/api/library/requests/{requestId}/library-feedback", requestId)
                        .with(duUser("librarian", "Library Specialist", "librarian"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "feedback", "Planned for procurement in the summer batch",
                                "expectedPurchaseMonth", "2026-08"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Feedback Provided"))
                .andExpect(jsonPath("$.expectedPurchaseMonth").value("2026-08"));

        var exportResponse = mockMvc.perform(get("/api/library/requests/{requestId}/export-form", requestId)
                        .with(duUser("librarian", "Library Specialist", "librarian")))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andReturn()
                .getResponse();

        assertThat(exportResponse.getContentAsByteArray()).isNotEmpty();
    }

    @Test
    void publishesSyllabusAfterColleagueAndDirectorApprovalAndCreatesLibraryRequest() throws Exception {
        var syllabusResponse = mockMvc.perform(post("/api/syllabi")
                        .with(duUser("teacher", "Aigerim Sadykova", "teacher"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("courseId", "syllabus-public-policy-2026"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode createdSyllabus = objectMapper.readTree(syllabusResponse);
        String syllabusId = createdSyllabus.path("id").asText();
        assertThat(syllabusId).isNotBlank();

        mockMvc.perform(put("/api/syllabi/{syllabusId}/director", syllabusId)
                        .with(duUser("teacher", "Aigerim Sadykova", "teacher"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("directorUsername", "director"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.directorUsername").value("director"));

        mockMvc.perform(put("/api/syllabi/{syllabusId}/director", syllabusId)
                        .with(duUser("teacher", "Aigerim Sadykova", "teacher"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("directorUsername", "director-business"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.directorUsername").value("director-business"));

        mockMvc.perform(put("/api/syllabi/{syllabusId}/director", syllabusId)
                        .with(duUser("teacher", "Aigerim Sadykova", "teacher"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("directorUsername", "director"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.directorUsername").value("director"));

        mockMvc.perform(put("/api/syllabi/{syllabusId}/reviewers", syllabusId)
                        .with(duUser("teacher", "Aigerim Sadykova", "teacher"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reviewerUsernames", List.of("teacher-colleague", "teacher-business")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.colleagueApprovals.length()").value(2))
                .andExpect(jsonPath("$.colleagueApprovals[?(@.username == 'teacher-colleague' && @.approved == false)]").isNotEmpty())
                .andExpect(jsonPath("$.colleagueApprovals[?(@.username == 'teacher-business' && @.approved == false)]").isNotEmpty());

        mockMvc.perform(put("/api/syllabi/{syllabusId}/reviewers", syllabusId)
                        .with(duUser("teacher", "Aigerim Sadykova", "teacher"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reviewerUsernames", List.of("director")))))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason("School director cannot be added as a colleague reviewer because director approval happens last"));

        var completedContent = buildCompleteSyllabusContent(
                (ObjectNode) createdSyllabus.path("content").deepCopy(),
                "Public Policy Analysis and Design",
                "PPA 302",
                "Public Administration and Policy",
                "Spring",
                "Policy Design Handbook"
        );

        mockMvc.perform(put("/api/syllabi/{syllabusId}", syllabusId)
                        .with(duUser("teacher", "Aigerim Sadykova", "teacher"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completedContent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progress").value(100));

        mockMvc.perform(post("/api/syllabi/{syllabusId}/submit-review", syllabusId)
                        .with(duUser("teacher", "Aigerim Sadykova", "teacher")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Pending Colleague Confirmation"));

        mockMvc.perform(get("/api/syllabi/review-queue")
                        .with(duUser("director", "Public Policy Director", "director")))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        mockMvc.perform(post("/api/syllabi/{syllabusId}/colleague-approve", syllabusId)
                        .with(duUser("teacher-colleague", "Colleague Teacher", "teacher")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Pending Colleague Confirmation"))
                .andExpect(jsonPath("$.colleagueApprovals[?(@.username == 'teacher-colleague' && @.approved == true)]").isNotEmpty())
                .andExpect(jsonPath("$.colleagueApprovals[?(@.username == 'teacher-business' && @.approved == false)]").isNotEmpty());

        mockMvc.perform(post("/api/syllabi/{syllabusId}/colleague-approve", syllabusId)
                        .with(duUser("teacher-business", "Business Teacher", "teacher")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Pending Director Review"))
                .andExpect(jsonPath("$.colleagueApprovals[?(@.username == 'teacher-colleague' && @.approved == true)]").isNotEmpty())
                .andExpect(jsonPath("$.colleagueApprovals[?(@.username == 'teacher-business' && @.approved == true)]").isNotEmpty());

        mockMvc.perform(get("/api/syllabi/review-queue")
                        .with(duUser("director", "Public Policy Director", "director")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(syllabusId))
                .andExpect(jsonPath("$[0].colleagueApprovals[0].approved").value(true));

        var approvedResponse = mockMvc.perform(post("/api/syllabi/{syllabusId}/approve", syllabusId)
                        .with(duUser("director", "Public Policy Director", "director")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Published"))
                .andExpect(jsonPath("$.linkedLibraryRequestId").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String linkedLibraryRequestId = objectMapper.readTree(approvedResponse).path("linkedLibraryRequestId").asText();
        assertThat(linkedLibraryRequestId).isNotBlank();

        mockMvc.perform(get("/api/syllabi/{syllabusId}", syllabusId)
                        .with(duUser("teacher", "Aigerim Sadykova", "teacher")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(syllabusId))
                .andExpect(jsonPath("$.status").value("Published"));

        mockMvc.perform(get("/api/library/requests")
                        .with(duUser("librarian", "Library Specialist", "librarian")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(linkedLibraryRequestId))
                .andExpect(jsonPath("$[0].syllabusId").value(syllabusId));

        var publishedPdf = mockMvc.perform(get("/api/syllabi/{syllabusId}/export-pdf", syllabusId)
                        .with(duUser("teacher", "Aigerim Sadykova", "teacher")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        try (var document = PDDocument.load(new ByteArrayInputStream(publishedPdf))) {
            var pdfText = new PDFTextStripper().getText(document).replaceAll("\\s+", " ").trim();
            assertThat(pdfText)
                    .contains("Approved")
                    .contains("Approved electronically")
                    .contains("By Director Dana Utegenova")
                    .contains("Public Policy Analysis and Design")
                    .contains("PPA 302")
                    .contains("Aigerim Sadykova")
                    .contains("Policy Design Handbook")
                    .contains("Gross Domestic Product")
                    .contains("Policy analysis frameworks")
                    .contains("Policy design, implementation, and evaluation")
                    .contains("Consultation and support are available")
                    .contains("Submit the report and supporting materials in Moodle");
        }
    }

    @Test
    void exposesDisciplineTagsAndPdfExport() throws Exception {
        mockMvc.perform(get("/api/courses")
                        .with(duUser("teacher", "Aigerim Sadykova", "teacher")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].disciplineTags").isArray())
                .andExpect(jsonPath("$[0].schoolId").exists());

        mockMvc.perform(get("/api/library/disciplines")
                        .with(duUser("teacher", "Aigerim Sadykova", "teacher")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].disciplineTags").isArray())
                .andExpect(jsonPath("$[0].courseId").exists());

        mockMvc.perform(get("/api/library/books")
                        .param("query", "macro")
                        .with(duUser("teacher", "Aigerim Sadykova", "teacher")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").exists())
                .andExpect(jsonPath("$[0].discipline").exists());

        mockMvc.perform(get("/api/library/books")
                        .param("query", "макроэкономика")
                        .with(duUser("teacher", "Aigerim Sadykova", "teacher")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Макроэкономика"));

        mockMvc.perform(get("/api/library/books")
                        .param("query", "мемлекеттік")
                        .with(duUser("teacher", "Aigerim Sadykova", "teacher")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Мемлекеттік саясатты талдау"));

        mockMvc.perform(get("/api/library/book-tags")
                        .with(duUser("teacher", "Aigerim Sadykova", "teacher")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].value").exists())
                .andExpect(jsonPath("$[0].label").exists())
                .andExpect(jsonPath("$[0].booksCount").isNumber());

        mockMvc.perform(get("/api/library/book-tags")
                        .param("search", "policy")
                        .with(duUser("teacher", "Aigerim Sadykova", "teacher")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].value").value("policy analysis"));

        var syllabusResponse = mockMvc.perform(post("/api/syllabi")
                        .with(duUser("teacher", "Aigerim Sadykova", "teacher"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String syllabusId = objectMapper.readTree(syllabusResponse).path("id").asText();
        var pdfResponse = mockMvc.perform(get("/api/syllabi/{syllabusId}/export-pdf", syllabusId)
                        .with(duUser("teacher", "Aigerim Sadykova", "teacher")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn()
                .getResponse();

        assertThat(new String(pdfResponse.getContentAsByteArray(), 0, 4)).isEqualTo("%PDF");

        try (var document = PDDocument.load(new ByteArrayInputStream(pdfResponse.getContentAsByteArray()))) {
            var text = new PDFTextStripper().getText(document);
            assertThat(text)
                    .contains("General information")
                    .contains("Goals, objectives and learning outcomes")
                    .contains("Course Policies")
                    .contains("Course Schedule")
                    .contains("For approval")
                    .contains("By Director Dana Utegenova");
        }
    }

    private ObjectNode buildCompleteSyllabusContent(
            ObjectNode content,
            String title,
            String code,
            String program,
            String trimester,
            String resourceTitle
    ) {
        content.put("title", title);
        content.put("code", code);
        content.put("degreeLevel", "Bachelor");
        content.put("program", program);
        content.put("academicYear", "2026-2027");
        content.put("trimester", trimester);
        content.put("languageOfInstruction", "English");
        content.put("credits", 5);
        content.put("prerequisites", "Introduction to public administration");
        content.put("postrequisites", "Advanced public governance");
        content.put("overview", "This course covers public policy analysis, institutional design, and governance tools.");
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
        content.set("goals", stringArray("Understand public policy tools"));
        content.set("learningOutcomes", stringArray("Evaluate policy choices and implementation tradeoffs"));
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
                .put("author", "John Policy")
                .put("year", "2024")
                .put("type", "Textbook")
                .put("publisher", "Global Governance Press")
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
                .put("week", 1)
                .put("topic", "Policy analysis frameworks");
        content.set("weeklyPlan", weeklyPlan);

        var detailedPlan = objectMapper.createArrayNode();
        detailedPlan.addObject()
                .put("week", 1)
                .put("lectureTopics", "Policy design, implementation, and evaluation")
                .put("practiceTopics", "Policy case workshop")
                .put("independentWork", "Read the assigned policy case")
                .put("assessment", "Case analysis submission");
        content.set("detailedPlan", detailedPlan);

        var optionalSections = objectMapper.createArrayNode();
        optionalSections.addObject()
                .put("title", "Student support")
                .set("content", objectMapper.createObjectNode()
                        .put("kind", "richText")
                        .put("html", "<p>Consultation and support are available during office hours.</p>"));
        content.set("optionalSections", optionalSections);

        var customSections = objectMapper.createArrayNode();
        var blocks = objectMapper.createArrayNode();
        blocks.addObject()
                .put("heading", "Submission")
                .put("body", "Submit the report and supporting materials in Moodle.");
        var structuredContent = objectMapper.createObjectNode();
        structuredContent.put("kind", "structured");
        structuredContent.put("intro", "Assessment evidence must be reproducible.");
        structuredContent.set("blocks", blocks);
        customSections.addObject()
                .put("title", "Assessment notes")
                .set("content", structuredContent);
        content.set("customSections", customSections);
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

    private JsonNode readJson(RequestBuilder requestBuilder) throws Exception {
        var response = mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response);
    }

    private Set<String> collectTexts(JsonNode array, String fieldName) {
        var values = new LinkedHashSet<String>();
        for (JsonNode item : array) {
            var value = item.path(fieldName).asText();
            if (!value.isBlank()) {
                values.add(value);
            }
        }
        return values;
    }

    private long countByFieldValue(JsonNode array, String fieldName, String expectedValue) {
        long count = 0;
        for (JsonNode item : array) {
            if (expectedValue.equals(item.path(fieldName).asText())) {
                count++;
            }
        }
        return count;
    }

    private RequestPostProcessor duUser(String username, String displayName, String role) {
        return request -> {
            var token = token(Map.of(
                    "sub", Math.abs(username.hashCode()),
                    "email", username,
                    "name", displayName,
                    "roles", List.of(role),
                    "exp", Instant.now().plusSeconds(300).getEpochSecond()
            ));
            request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            return request;
        };
    }

    private String token(Map<String, Object> claims) {
        try {
            var header = Map.of("alg", "HS256", "typ", "JWT");
            var signingInput = base64Url(objectMapper.writeValueAsBytes(header))
                    + "." + base64Url(objectMapper.writeValueAsBytes(claims));
            return signingInput + "." + signature(signingInput);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to build test Digital University JWT", ex);
        }
    }

    private String signature(String signingInput) throws Exception {
        var mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return base64Url(mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));
    }

    private String base64Url(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }
}

