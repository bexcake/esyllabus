package kz.iqadam.esyllabus.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "digital-university.jwt.enabled=true",
        "digital-university.jwt.secret=dGVzdC1kdS1zZWNyZXQ="
})
class DigitalUniversityJwtAuthenticationTests {

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
    void acceptsDigitalUniversityBearerToken() throws Exception {
        var token = token(Map.of(
                "sub", 1001,
                "email", "teacher@astanait.edu.kz",
                "name", "AITU Teacher",
                "roles", List.of("lecturer"),
                "exp", Instant.now().plusSeconds(300).getEpochSecond()
        ));

        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("teacher@astanait.edu.kz"))
                .andExpect(jsonPath("$.displayName").value("AITU Teacher"))
                .andExpect(jsonPath("$.roles[0]").value("TEACHER"))
                .andExpect(jsonPath("$.employeeId").value(1001));
    }

    @Test
    void exposesSameProfileOnShortMeEndpoint() throws Exception {
        var token = token(Map.of(
                "sub", 1002,
                "email", "director@astanait.edu.kz",
                "name", "AITU Director",
                "roles", List.of("director"),
                "exp", Instant.now().plusSeconds(300).getEpochSecond()
        ));

        mockMvc.perform(get("/api/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("director@astanait.edu.kz"))
                .andExpect(jsonPath("$.roles[0]").value("DIRECTOR"))
                .andExpect(jsonPath("$.employeeId").value(1002));
    }

    @Test
    void rejectsTokenWithoutEmployeeIdSubject() throws Exception {
        var token = token(Map.of(
                "email", "teacher@astanait.edu.kz",
                "roles", List.of("teacher"),
                "exp", Instant.now().plusSeconds(300).getEpochSecond()
        ));

        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid Digital University bearer token"));
    }

    @Test
    void rejectsInvalidDigitalUniversityBearerTokenSignature() throws Exception {
        var token = token(Map.of(
                "sub", 1001,
                "email", "teacher@astanait.edu.kz",
                "roles", List.of("teacher"),
                "exp", Instant.now().plusSeconds(300).getEpochSecond()
        ));

        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token + "broken"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid Digital University bearer token"));
    }

    @Test
    void doesNotExposeHttpBasicAuthentication() throws Exception {
        var credentials = Base64.getEncoder()
                .encodeToString("legacy-user:legacy-password".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE));
    }

    private String token(Map<String, Object> claims) throws Exception {
        var header = Map.of("alg", "HS256", "typ", "JWT");
        var signingInput = base64Url(objectMapper.writeValueAsBytes(header))
                + "." + base64Url(objectMapper.writeValueAsBytes(claims));
        return signingInput + "." + signature(signingInput);
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
