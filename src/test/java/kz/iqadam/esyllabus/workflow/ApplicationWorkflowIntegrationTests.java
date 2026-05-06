package kz.iqadam.esyllabus.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
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
                .andExpect(jsonPath("$[0].cabinet").exists());

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
                        Map.entry("literatureType", "Учебная литература")
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

        mockMvc.perform(post("/api/library/requests/{requestId}/submit", requestId)
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("teacher", "teacher123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Pending Director Approval"));

        mockMvc.perform(post("/api/library/requests/{requestId}/director-approve", requestId)
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("director", "director123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Approved by Director"));

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
}
