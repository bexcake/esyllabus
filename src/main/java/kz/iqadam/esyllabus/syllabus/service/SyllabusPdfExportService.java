package kz.iqadam.esyllabus.syllabus.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import kz.iqadam.esyllabus.security.CurrentUser;
import kz.iqadam.esyllabus.syllabus.api.SyllabusResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SyllabusPdfExportService {

    private final SyllabusService syllabusService;

    public SyllabusPdfExportService(SyllabusService syllabusService) {
        this.syllabusService = syllabusService;
    }

    public byte[] exportSyllabus(CurrentUser user, String syllabusId) {
        SyllabusResponse syllabus = syllabusService.getSyllabus(user, syllabusId);
        var html = buildHtml(syllabus);

        try (var output = new ByteArrayOutputStream()) {
            var builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(output);
            builder.run();
            return output.toByteArray();
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to export syllabus to PDF");
        }
    }

    private String buildHtml(SyllabusResponse syllabus) {
        var content = syllabus.content();
        var goals = listSection(content.path("goals"));
        var learningOutcomes = listSection(content.path("learningOutcomes"));
        var methods = listSection(content.path("teachingMethods"));
        var resources = resourceSection(content.path("resources"));

        return """
                <html>
                <head>
                    <meta charset="UTF-8" />
                    <style>
                        body { font-family: Arial, sans-serif; color: #1f2933; font-size: 12px; }
                        h1, h2 { color: #0f4c5c; }
                        table { width: 100%%; border-collapse: collapse; margin-bottom: 16px; }
                        th, td { border: 1px solid #c8d1dc; padding: 6px; vertical-align: top; }
                        .meta { margin-bottom: 16px; }
                        .section { margin-bottom: 18px; }
                    </style>
                </head>
                <body>
                    <h1>%s</h1>
                    <div class="meta">
                        <p><strong>Code:</strong> %s</p>
                        <p><strong>Program:</strong> %s</p>
                        <p><strong>Status:</strong> %s</p>
                        <p><strong>Owner:</strong> %s</p>
                    </div>
                    <div class="section">
                        <h2>Overview</h2>
                        <p>%s</p>
                    </div>
                    <div class="section">
                        <h2>Workload</h2>
                        <table>
                            <tr><th>Lectures</th><th>Practice</th><th>Lab</th><th>IASS</th><th>SIS</th><th>Total</th></tr>
                            <tr><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr>
                        </table>
                    </div>
                    <div class="section">
                        <h2>Goals</h2>
                        %s
                    </div>
                    <div class="section">
                        <h2>Learning Outcomes</h2>
                        %s
                    </div>
                    <div class="section">
                        <h2>Teaching Methods</h2>
                        %s
                    </div>
                    <div class="section">
                        <h2>Resources</h2>
                        %s
                    </div>
                </body>
                </html>
                """.formatted(
                escape(text(content, "title", syllabus.id())),
                escape(text(content, "code", "")),
                escape(text(content, "program", "")),
                escape(syllabus.status()),
                escape(syllabus.ownerEmail()),
                escape(stripHtml(text(content, "overview", ""))),
                escape(content.path("workload").path("lecturesHours").asText("0")),
                escape(content.path("workload").path("practiceHours").asText("0")),
                escape(content.path("workload").path("labHours").asText("0")),
                escape(content.path("workload").path("iassHours").asText("0")),
                escape(content.path("workload").path("sisHours").asText("0")),
                escape(content.path("workload").path("totalHours").asText("0")),
                goals,
                learningOutcomes,
                methods,
                resources
        );
    }

    private String listSection(JsonNode arrayNode) {
        if (!arrayNode.isArray() || arrayNode.isEmpty()) {
            return "<p>No data provided.</p>";
        }
        List<String> rows = new ArrayList<>();
        for (var item : arrayNode) {
            var text = escape(stripHtml(item.asText("")));
            if (!text.isBlank()) {
                rows.add("<li>" + text + "</li>");
            }
        }
        return rows.isEmpty() ? "<p>No data provided.</p>" : "<ul>" + String.join("", rows) + "</ul>";
    }

    private String resourceSection(JsonNode arrayNode) {
        if (!arrayNode.isArray() || arrayNode.isEmpty()) {
            return "<p>No resources listed.</p>";
        }
        var rows = new ArrayList<String>();
        for (var item : arrayNode) {
            rows.add("""
                    <tr>
                        <td>%s</td>
                        <td>%s</td>
                        <td>%s</td>
                        <td>%s</td>
                    </tr>
                    """.formatted(
                    escape(item.path("title").asText("")),
                    escape(item.path("author").asText("")),
                    escape(item.path("year").asText("")),
                    escape(item.path("type").asText(""))
            ));
        }
        return """
                <table>
                    <tr><th>Title</th><th>Author</th><th>Year</th><th>Type</th></tr>
                    %s
                </table>
                """.formatted(String.join("", rows));
    }

    private String text(JsonNode node, String field, String fallback) {
        var value = node.path(field).asText("").trim();
        return value.isBlank() ? fallback : value;
    }

    private String stripHtml(String value) {
        return value.replaceAll("<[^>]+>", "").trim();
    }

    private String escape(String value) {
        return value == null ? "" : new String(value.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
