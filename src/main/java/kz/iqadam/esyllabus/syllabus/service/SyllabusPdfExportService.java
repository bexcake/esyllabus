package kz.iqadam.esyllabus.syllabus.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.svgsupport.BatikSVGDrawer;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import kz.iqadam.esyllabus.directory.service.DirectoryService;
import kz.iqadam.esyllabus.security.CurrentUser;
import kz.iqadam.esyllabus.syllabus.api.SyllabusResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SyllabusPdfExportService {

    private static final String NOT_PROVIDED = "Not provided.";
    private static final String OFFICIAL_LOGO_SVG = loadOfficialLogo();
    private static final DateTimeFormatter APPROVAL_DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy")
            .withZone(ZoneId.of("Asia/Almaty"));

    private final SyllabusService syllabusService;
    private final DirectoryService directoryService;

    public SyllabusPdfExportService(SyllabusService syllabusService, DirectoryService directoryService) {
        this.syllabusService = syllabusService;
        this.directoryService = directoryService;
    }

    public byte[] exportSyllabus(CurrentUser user, String syllabusId) {
        SyllabusResponse syllabus = syllabusService.getSyllabus(user, syllabusId);
        var html = buildHtml(syllabus);

        try (var output = new ByteArrayOutputStream()) {
            var builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.useSVGDrawer(new BatikSVGDrawer());
            registerFonts(builder);
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
        var html = new StringBuilder(32_000);

        html.append("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8" />
                    <style>
                        @page {
                            size: A4;
                            margin: 15mm 13mm 14mm 13mm;
                            @bottom-center {
                                content: "Page " counter(page) " of " counter(pages);
                                font-family: "DejaVu Serif", "Times New Roman", serif;
                                font-size: 8pt;
                                color: #444;
                            }
                        }
                        * { box-sizing: border-box; }
                        body {
                            font-family: "DejaVu Serif", "Times New Roman", serif;
                            color: #111;
                            font-size: 10.2pt;
                            line-height: 1.22;
                        }
                        .topline { border-top: 1.2px solid #111; height: 1px; margin-bottom: 18px; }
                        .header { min-height: 96px; position: relative; }
                        .brand { position: absolute; left: 0; top: 4px; width: 132px; height: 70px; }
                        .brand svg { display: block; width: 132px; height: 70px; }
                        .approval {
                            position: absolute;
                            right: 0;
                            top: 0;
                            width: 240px;
                            text-align: center;
                            font-size: 11pt;
                            line-height: 1.25;
                        }
                        .signature-line { border-bottom: 1px solid #111; height: 18px; margin: 1px 18px 2px; }
                        .title { text-align: center; margin: 26px 0 12px; }
                        .title h1 { margin: 0; font-size: 14.5pt; }
                        .title h2 { margin: 2px 0 0; font-size: 13pt; }
                        table { border-collapse: collapse; width: 100%; }
                        thead { display: table-header-group; }
                        tfoot { display: table-footer-group; }
                        tr { page-break-inside: avoid; }
                        .syllabus-table {
                            -fs-table-paginate: paginate;
                            table-layout: fixed;
                            border: 1.2px solid #111;
                            margin-bottom: 10px;
                        }
                        .schedule-table { page-break-before: always; }
                        .abbreviations-table,
                        .section-collection { page-break-inside: avoid; }
                        .syllabus-table th,
                        .syllabus-table td {
                            border: 0.8px solid #111;
                            padding: 4px 6px;
                            vertical-align: top;
                        }
                        .section-title {
                            text-align: center;
                            font-weight: bold;
                            background: #f5f5f5;
                            font-size: 10.6pt;
                        }
                        .label { width: 25%; }
                        .value { width: 75%; }
                        .numbered-label { width: 25%; }
                        .nested { font-size: 9.4pt; margin: 3px 0; }
                        .nested th, .nested td { border: 0.7px solid #111; padding: 3px 4px; text-align: left; vertical-align: top; }
                        .nested th { font-weight: bold; text-align: center; }
                        .center { text-align: center; }
                        p { margin: 0 0 4px; }
                        ul, ol { margin: 0 0 4px 18px; padding: 0; }
                        li { margin-bottom: 2px; }
                        .small { font-size: 9pt; }
                        .muted { color: #444; }
                        .page-break { page-break-before: always; }
                    </style>
                </head>
                <body>
                """);

        appendHeader(html, syllabus, content, directorDisplayName(syllabus, content));
        appendGeneralInformation(html, syllabus, content);
        appendCourseOutcomes(html, content);
        appendAbbreviations(html, content.path("abbreviations"));
        appendSchedule(html, content);
        appendCustomSections(html, content);

        html.append("""
                </body>
                </html>
                """);
        return html.toString();
    }

    private void appendHeader(StringBuilder html, SyllabusResponse syllabus, JsonNode content, String directorName) {
        var published = "Published".equalsIgnoreCase(syllabus.status());
        html.append("""
                <div class="topline"></div>
                <div class="header">
                    <div class="brand">
                """);
        html.append(OFFICIAL_LOGO_SVG);
        html.append("""
                    </div>
                    <div class="approval">
                """);
        html.append(published ? "<strong>&#171;Approved&#187;</strong><br />" : "<strong>&#171;For approval&#187;</strong><br />");
        html.append(escape(approvalRole(directorName))).append("<br />");
        if (published && syllabus.updatedAt() != null) {
            html.append("<div class=\"signature-line\"></div><span class=\"small\">Approved electronically</span><br />")
                    .append(escape(APPROVAL_DATE.format(syllabus.updatedAt())));
        } else {
            html.append("<div class=\"signature-line\"></div>&#171;____&#187; __________ 20____");
        }
        html.append("""
                    </div>
                </div>
                <div class="title">
                    <h1>Syllabus</h1>
                    <h2>Academic Year&#160;""");
        html.append(escape(text(content, "academicYear", "20__ - 20__")));
        html.append("""
                    </h2>
                </div>
                """);
    }

    private void appendGeneralInformation(StringBuilder html, SyllabusResponse syllabus, JsonNode content) {
        html.append("""
                <table class="syllabus-table">
                    <colgroup>
                        <col class="label" />
                        <col class="value" />
                    </colgroup>
                    <thead><tr><th colspan="2" class="section-title">1. General information</th></tr></thead>
                    <tbody>
                """);
        appendRow(html, "Course Code", text(content, "code", ""));
        appendRow(html, "Course Title", text(content, "title", syllabus.id()));
        appendRow(html, "Degree Cycle (Level)/<br />Major / Relation to curriculum", degreeAndProgram(content));
        appendRow(html, "Year, trimester", yearAndTrimester(content));
        appendRow(html, "Language of Instruction:", text(content, "languageOfInstruction", ""));
        appendRow(html, "Lecturer(s)/ Instructors/<br />Instructor Contact Information", instructors(content.path("instructors"), syllabus.ownerEmail()));
        appendRow(html, "Number of Credits", numberText(content.path("credits"), "0"));
        appendRowHtml(html, "Workload of<br />course components and<br />credits per trimester", workloadTable(content.path("workload"), content.path("credits")));
        appendRow(html, "Prerequisites", text(content, "prerequisites", ""));
        appendRow(html, "Post requisites", firstNonBlank(text(content, "postrequisites", ""), text(content, "post requisites", "")));
        html.append("</tbody></table>");
    }

    private void appendCourseOutcomes(StringBuilder html, JsonNode content) {
        html.append("""
                <table class="syllabus-table">
                    <colgroup>
                        <col class="numbered-label" />
                        <col class="value" />
                    </colgroup>
                    <thead><tr><th colspan="2" class="section-title">2. Goals, objectives and learning outcomes of the course</th></tr></thead>
                    <tbody>
                """);
        appendNumberedRow(html, "1.", "Course<br />Overview/Description", renderText(text(content, "overview", "")));
        appendNumberedRow(html, "2.", "Course Learning Goals", renderList(content.path("goals"), NOT_PROVIDED));
        appendNumberedRow(html, "3.", "Course Learning Outcomes", renderList(content.path("learningOutcomes"), NOT_PROVIDED));
        appendNumberedRow(html, "4.", "Methods/forms of<br />teaching", renderList(content.path("teachingMethods"), defaultTeachingMethods()));
        appendNumberedRow(html, "5.", "Coursework and<br />Grading Scheme", renderGradingSchema(content.path("gradingSchema")));
        appendNumberedRow(html, "6.", "Academic Integrity", renderText(firstNonBlank(
                text(content, "academicIntegrity", ""),
                defaultAcademicIntegrity()
        )));
        appendNumberedRow(html, "7.", "Learning resources:", renderResources(content.path("resources")));
        appendNumberedRow(html, "8.", "Technology employed:", renderList(content.path("technologyEmployed"), "Learning Management System: Moodle (moodle.astanait.edu.kz)."));
        appendNumberedRow(html, "9.", "Course Policies", renderCoursePolicies(content.path("coursePolicies")));
        appendNumberedRow(html, "10.", "Course Schedule", renderText("See the Course Schedule section below."));
        appendNumberedRow(html, "11.", "Syllabus Inclusion<br />Statements", renderText(firstNonBlank(
                text(content, "inclusionStatements", ""),
                defaultInclusionStatements()
        )));
        html.append("</tbody></table>");
    }

    private void appendAbbreviations(StringBuilder html, JsonNode abbreviations) {
        if (!abbreviations.isArray() || abbreviations.isEmpty()) {
            return;
        }
        html.append("""
                <table class="syllabus-table abbreviations-table">
                    <thead>
                        <tr><th colspan="3" class="section-title">3.1 Abbreviations</th></tr>
                        <tr><th class="center">#</th><th>Abbreviation</th><th>Meaning</th></tr>
                    </thead>
                    <tbody>
                """);
        var index = 1;
        for (var item : abbreviations) {
            html.append("<tr><td class=\"center\">").append(index++).append("</td><td>")
                    .append(escape(firstNonBlank(field(item, "shortForm"), field(item, "abbreviation"), field(item, "name"))))
                    .append("</td><td>")
                    .append(escape(firstNonBlank(field(item, "meaning"), field(item, "description"), field(item, "value"))))
                    .append("</td></tr>");
        }
        html.append("</tbody></table>");
    }

    private void appendSchedule(StringBuilder html, JsonNode content) {
        var weeklyPlan = content.path("weeklyPlan");
        var detailedPlan = content.path("detailedPlan");

        if ((!weeklyPlan.isArray() || weeklyPlan.isEmpty()) && (!detailedPlan.isArray() || detailedPlan.isEmpty())) {
            return;
        }

        html.append("""
                <table class="syllabus-table schedule-table">
                    <thead>
                        <tr><th colspan="6" class="section-title">3.2 Course Schedule</th></tr>
                        <tr>
                            <th class="center">Week</th>
                            <th>Topic</th>
                            <th>Lecture / Theory</th>
                            <th>Practice / Lab</th>
                            <th>Independent work</th>
                            <th>Assessment / Resources</th>
                        </tr>
                    </thead>
                    <tbody>
                """);

        var rowCount = Math.max(weeklyPlan.isArray() ? weeklyPlan.size() : 0, detailedPlan.isArray() ? detailedPlan.size() : 0);
        for (var index = 0; index < rowCount; index++) {
            var weeklyItem = weeklyPlan.path(index);
            var detailedItem = detailedPlan.path(index);
            html.append("<tr>")
                    .append("<td class=\"center\">").append(escape(firstNonBlank(
                            firstField(weeklyItem, "week"),
                            firstField(detailedItem, "week"),
                            String.valueOf(index + 1)
                    ))).append("</td>")
                    .append("<td>").append(renderMergedCell(weeklyItem, detailedItem, "topic", "module", "section", "title")).append("</td>")
                    .append("<td>").append(renderMergedCell(detailedItem, weeklyItem, "lectureTopics", "lecture", "theory", "description")).append("</td>")
                    .append("<td>").append(renderMergedCell(detailedItem, weeklyItem, "practiceTopics", "practice", "labTopics", "laboratory", "seminar")).append("</td>")
                    .append("<td>").append(renderMergedCell(detailedItem, weeklyItem, "iass", "sis", "independentWork", "homework")).append("</td>")
                    .append("<td>").append(renderMergedCell(detailedItem, weeklyItem, "assessment", "resources", "reading", "deliverables")).append("</td>")
                    .append("</tr>");
        }
        html.append("</tbody></table>");
    }

    private void appendCustomSections(StringBuilder html, JsonNode content) {
        appendSectionCollection(html, "Additional Sections", content.path("optionalSections"));
        appendSectionCollection(html, "Custom Sections", content.path("customSections"));
    }

    private void appendSectionCollection(StringBuilder html, String title, JsonNode sections) {
        if (!sections.isArray() || sections.isEmpty()) {
            return;
        }
        html.append("<table class=\"syllabus-table section-collection\"><thead><tr><th colspan=\"2\" class=\"section-title\">")
                .append(escape(title))
                .append("</th></tr></thead><tbody>");
        for (var section : sections) {
            appendRowHtml(
                    html,
                    escape(firstNonBlank(field(section, "title"), field(section, "name"), "Section")),
                    renderSectionContent(section)
            );
        }
        html.append("</tbody></table>");
    }

    private String workloadTable(JsonNode workload, JsonNode creditsNode) {
        return """
                <table class="nested">
                    <tr>
                        <th rowspan="2">ECTS<br />credits</th>
                        <th colspan="3">Contact hours</th>
                        <th rowspan="2">IASS</th>
                        <th rowspan="2">SIS</th>
                        <th rowspan="2">Total hours</th>
                    </tr>
                    <tr>
                        <th>Lectures</th>
                        <th>Practice<br />sessions</th>
                        <th>Laboratory<br />work</th>
                    </tr>
                    <tr>
                        <td class="center">%s</td>
                        <td class="center">%s</td>
                        <td class="center">%s</td>
                        <td class="center">%s</td>
                        <td class="center">%s</td>
                        <td class="center">%s</td>
                        <td class="center">%s</td>
                    </tr>
                </table>
                """.formatted(
                escape(numberText(creditsNode, "0")),
                escape(numberText(workload.path("lecturesHours"), "0")),
                escape(numberText(workload.path("practiceHours"), "0")),
                escape(numberText(workload.path("labHours"), "0")),
                escape(numberText(workload.path("iassHours"), "0")),
                escape(numberText(workload.path("sisHours"), "0")),
                escape(numberText(workload.path("totalHours"), "0"))
        );
    }

    private String renderGradingSchema(JsonNode gradingSchema) {
        if (!gradingSchema.isArray() || gradingSchema.isEmpty()) {
            return renderText("See the assessment rules and assignments published in Moodle.");
        }

        var structured = false;
        for (var item : gradingSchema) {
            if (item.isObject()) {
                structured = true;
                break;
            }
        }
        if (!structured) {
            return renderList(gradingSchema, NOT_PROVIDED);
        }

        var html = new StringBuilder("""
                <table class="nested">
                    <tr><th>#</th><th>Assessment</th><th>Weight</th><th>Description</th></tr>
                """);
        var index = 1;
        for (var item : gradingSchema) {
            html.append("<tr><td class=\"center\">").append(index++).append("</td><td>")
                    .append(escape(firstNonBlank(field(item, "name"), field(item, "title"), "Assessment")))
                    .append("</td><td class=\"center\">")
                    .append(escape(weight(item.path("weight"))))
                    .append("</td><td>")
                    .append(renderText(firstNonBlank(field(item, "description"), field(item, "notes"), "")))
                    .append("</td></tr>");
        }
        html.append("</table>");
        return html.toString();
    }

    private String renderResources(JsonNode resources) {
        if (!resources.isArray() || resources.isEmpty()) {
            return renderText("Learning resources are provided by the instructor through Moodle and the university library.");
        }

        var html = new StringBuilder("""
                <table class="nested">
                    <tr>
                        <th>#</th>
                        <th>Title</th>
                        <th>Author</th>
                        <th>Year</th>
                        <th>Type</th>
                    </tr>
                """);
        var index = 1;
        for (var item : resources) {
            html.append("<tr><td class=\"center\">").append(index++).append("</td><td>")
                    .append(resourceTitle(item))
                    .append("</td><td>")
                    .append(escape(field(item, "author")))
                    .append("</td><td class=\"center\">")
                    .append(escape(firstNonBlank(field(item, "year"), field(item, "publicationYear"))))
                    .append("</td><td>")
                    .append(escape(firstNonBlank(field(item, "type"), item.path("isRequired").asBoolean(false) ? "Main" : "Additional")))
                    .append("</td></tr>");
        }
        html.append("</table>");
        return html.toString();
    }

    private String renderCoursePolicies(JsonNode policies) {
        var rows = new ArrayList<String>();
        addPolicy(rows, "Attendance", firstNonBlank(field(policies, "attendance"), defaultAttendancePolicy()));
        addPolicy(rows, "Late submissions", firstNonBlank(field(policies, "lateSubmissions"), defaultLateSubmissionPolicy()));
        addPolicy(rows, "Attestation I and II", firstNonBlank(field(policies, "examsAttestation"), defaultExamPolicy()));
        addPolicy(rows, "Classroom behavior", firstNonBlank(field(policies, "classroomBehavior"), defaultClassroomPolicy()));
        addPolicy(rows, "Communication", firstNonBlank(field(policies, "communicationPolicy"), "Students are encouraged to contact the instructor by email or during office hours."));
        addPolicy(rows, "Other policies", field(policies, "otherPolicies"));
        return String.join("", rows);
    }

    private void addPolicy(List<String> rows, String title, String value) {
        var normalized = normalized(value);
        if (normalized != null) {
            rows.add("<p><strong>" + escape(title) + ":</strong> " + inlineText(normalized) + "</p>");
        }
    }

    private String renderList(JsonNode arrayNode, String fallback) {
        if (!arrayNode.isArray() || arrayNode.isEmpty()) {
            return renderText(fallback);
        }
        var rows = new ArrayList<String>();
        for (var item : arrayNode) {
            var text = normalized(itemText(item));
            if (text != null) {
                rows.add("<li>" + inlineText(text) + "</li>");
            }
        }
        return rows.isEmpty() ? renderText(fallback) : "<ul>" + String.join("", rows) + "</ul>";
    }

    private String renderText(String value) {
        var normalized = normalized(stripHtml(value));
        if (normalized == null) {
            return "<p>" + NOT_PROVIDED + "</p>";
        }
        var paragraphs = new ArrayList<String>();
        for (var line : normalized.split("\\R+")) {
            var item = normalized(line);
            if (item != null) {
                paragraphs.add("<p>" + inlineText(item) + "</p>");
            }
        }
        return paragraphs.isEmpty() ? "<p>" + NOT_PROVIDED + "</p>" : String.join("", paragraphs);
    }

    private String renderMergedCell(JsonNode primary, JsonNode secondary, String... fields) {
        return renderText(firstNonBlank(firstField(primary, fields), firstField(secondary, fields)));
    }

    private String renderSectionContent(JsonNode section) {
        if (!section.isObject()) {
            return renderText(itemText(section));
        }
        var items = section.path("items");
        if (items.isArray() && !items.isEmpty()) {
            return renderList(items, NOT_PROVIDED);
        }

        var content = section.path("content");
        if (content.isObject()) {
            return switch (field(content, "kind")) {
                case "richText" -> renderText(field(content, "html"));
                case "links" -> renderLinks(content.path("items"));
                case "table" -> renderAdditionalTable(content);
                case "list" -> renderList(content.path("items"), NOT_PROVIDED);
                case "structured" -> renderStructuredContent(content);
                default -> renderText(itemText(content));
            };
        }
        return renderText(firstNonBlank(
                field(section, "content"),
                field(section, "description"),
                field(section, "text"),
                field(section, "value")
        ));
    }

    private String renderLinks(JsonNode links) {
        if (!links.isArray() || links.isEmpty()) {
            return renderText(NOT_PROVIDED);
        }
        var rows = new ArrayList<String>();
        for (var link : links) {
            var label = firstNonBlank(field(link, "label"), field(link, "title"), field(link, "name"));
            var url = firstNonBlank(field(link, "url"), field(link, "href"));
            var value = joinNonBlank(label, url);
            if (normalized(value) != null) {
                rows.add("<li>" + inlineText(value) + "</li>");
            }
        }
        return rows.isEmpty() ? renderText(NOT_PROVIDED) : "<ul>" + String.join("", rows) + "</ul>";
    }

    private String renderAdditionalTable(JsonNode content) {
        var rows = content.path("rows");
        if (!rows.isArray() || rows.isEmpty()) {
            return renderText(NOT_PROVIDED);
        }

        var headers = content.path("columns");
        if (!headers.isArray() || headers.isEmpty()) {
            headers = content.path("headers");
        }

        var html = new StringBuilder("<table class=\"nested\">");
        if (headers.isArray() && !headers.isEmpty()) {
            html.append("<tr>");
            for (var header : headers) {
                html.append("<th>").append(escape(firstNonBlank(
                        itemText(header),
                        field(header, "label"),
                        field(header, "key")
                ))).append("</th>");
            }
            html.append("</tr>");
        }
        for (var row : rows) {
            if (!row.isArray()) {
                continue;
            }
            html.append("<tr>");
            for (var cell : row) {
                html.append("<td>").append(renderText(itemText(cell))).append("</td>");
            }
            html.append("</tr>");
        }
        return html.append("</table>").toString();
    }

    private String renderStructuredContent(JsonNode content) {
        var html = new StringBuilder();
        var intro = normalized(field(content, "intro"));
        if (intro != null) {
            html.append(renderText(intro));
        }
        var blocks = content.path("blocks");
        if (blocks.isArray()) {
            for (var block : blocks) {
                var heading = normalized(field(block, "heading"));
                var body = normalized(field(block, "body"));
                if (heading != null) {
                    html.append("<p><strong>").append(escape(heading)).append("</strong></p>");
                }
                if (body != null) {
                    html.append(renderText(body));
                }
            }
        }
        return html.isEmpty() ? renderText(NOT_PROVIDED) : html.toString();
    }

    private String resourceTitle(JsonNode item) {
        var title = firstNonBlank(field(item, "title"), field(item, "name"), NOT_PROVIDED);
        var details = new ArrayList<String>();
        addIfPresent(details, "ISBN", field(item, "isbn"));
        addIfPresent(details, "Publisher", field(item, "publisher"));
        addIfPresent(details, "URL", field(item, "url"));
        addIfPresent(details, "Notes", field(item, "notes"));
        if (details.isEmpty()) {
            return escape(title);
        }
        return escape(title) + "<br /><span class=\"small muted\">" + escape(String.join("; ", details)) + "</span>";
    }

    private void appendRow(StringBuilder html, String label, String value) {
        appendRowHtml(html, label, renderText(value));
    }

    private void appendRowHtml(StringBuilder html, String label, String valueHtml) {
        html.append("<tr><td>")
                .append(label)
                .append("</td><td>")
                .append(valueHtml)
                .append("</td></tr>");
    }

    private void appendNumberedRow(StringBuilder html, String number, String label, String valueHtml) {
        html.append("<tr><td><table class=\"nested\"><tr><td class=\"center\" style=\"width:24px;border:0;\">")
                .append(escape(number))
                .append("</td><td style=\"border:0;\">")
                .append(label)
                .append("</td></tr></table></td><td>")
                .append(valueHtml)
                .append("</td></tr>");
    }

    private String directorDisplayName(SyllabusResponse syllabus, JsonNode content) {
        var nameFromContent = firstNonBlank(
                text(content, "directorFullName", ""),
                text(content, "directorName", ""),
                text(content, "approvedBy", "")
        );
        if (normalized(nameFromContent) != null) {
            return nameFromContent;
        }

        var username = normalized(syllabus.directorUsername());
        if (username == null || directoryService == null) {
            return username;
        }
        try {
            return firstNonBlank(directoryService.getStaffByUsername(username).fullName(), username);
        } catch (ResponseStatusException exception) {
            return username;
        }
    }

    private String approvalRole(String directorName) {
        var director = normalized(directorName);
        return director == null ? "Director / Dean" : "By Director " + director;
    }

    private String degreeAndProgram(JsonNode content) {
        return firstNonBlank(
                text(content, "degreeCycle", ""),
                joinNonBlank(text(content, "degreeLevel", ""), text(content, "program", "")),
                text(content, "program", "")
        );
    }

    private String yearAndTrimester(JsonNode content) {
        return joinNonBlank(
                firstNonBlank(text(content, "year", ""), text(content, "courseYear", "")),
                text(content, "trimester", "")
        );
    }

    private String instructors(JsonNode instructors, String fallbackEmail) {
        if (!instructors.isArray() || instructors.isEmpty()) {
            return fallbackEmail;
        }
        var rows = new ArrayList<String>();
        for (var instructor : instructors) {
            var name = firstNonBlank(field(instructor, "fullName"), field(instructor, "name"), "");
            var position = field(instructor, "position");
            var email = field(instructor, "email");
            var office = firstNonBlank(field(instructor, "officeOrContact"), field(instructor, "cabinet"), field(instructor, "workplace"));
            var parts = new ArrayList<String>();
            addIfPresent(parts, null, name);
            addIfPresent(parts, null, position);
            addIfPresent(parts, null, email);
            addIfPresent(parts, null, office);
            if (!parts.isEmpty()) {
                rows.add(String.join(", ", parts));
            }
        }
        return rows.isEmpty() ? fallbackEmail : String.join("\n", rows);
    }

    private String firstField(JsonNode item, String... fields) {
        for (var field : fields) {
            var value = field(item, field);
            if (normalized(value) != null) {
                return value;
            }
        }
        return "";
    }

    private String itemText(JsonNode item) {
        if (item == null || item.isMissingNode() || item.isNull()) {
            return "";
        }
        if (item.isTextual() || item.isNumber() || item.isBoolean()) {
            return item.asText("");
        }
        if (item.isObject()) {
            return firstNonBlank(
                    field(item, "text"),
                    field(item, "value"),
                    field(item, "description"),
                    field(item, "name"),
                    field(item, "title"),
                    field(item, "topic"),
                    field(item, "lectureTopics")
            );
        }
        return "";
    }

    private String text(JsonNode node, String field, String fallback) {
        return firstNonBlank(field(node, field), fallback);
    }

    private String field(JsonNode node, String field) {
        if (node == null || node.isMissingNode() || node.isNull() || !node.has(field)) {
            return "";
        }
        var value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return "";
        }
        return value.isTextual() || value.isNumber() || value.isBoolean() ? value.asText("") : itemText(value);
    }

    private String numberText(JsonNode node, String fallback) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return fallback;
        }
        if (node.isNumber()) {
            return node.numberValue().toString();
        }
        var value = normalized(node.asText(""));
        return value == null ? fallback : value;
    }

    private String weight(JsonNode node) {
        var value = numberText(node, "");
        if (value.isBlank()) {
            return "";
        }
        return value.endsWith("%") ? value : value + "%";
    }

    private String joinNonBlank(String... values) {
        var result = new ArrayList<String>();
        for (var value : values) {
            var normalized = normalized(value);
            if (normalized != null) {
                result.add(normalized);
            }
        }
        return String.join("; ", result);
    }

    private String firstNonBlank(String... values) {
        for (var value : values) {
            var normalized = normalized(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return "";
    }

    private String normalized(String value) {
        var result = Objects.toString(value, "").trim();
        return result.isBlank() ? null : result;
    }

    private String stripHtml(String value) {
        return Objects.toString(value, "")
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("<[^>]+>", "")
                .trim();
    }

    private String inlineText(String value) {
        return escape(value).replace("\n", "<br />");
    }

    private String escape(String value) {
        return value == null ? "" : new String(value.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private void addIfPresent(List<String> values, String label, String value) {
        var normalized = normalized(value);
        if (normalized != null) {
            values.add(label == null ? normalized : label + ": " + normalized);
        }
    }

    private String defaultTeachingMethods() {
        return "Lectures, practical sessions, group discussions, independent work and project-based learning.";
    }

    private String defaultAcademicIntegrity() {
        return "Follow the Rules of Academic Integrity approved by the Scientific Council of LLP \"Astana IT University\". "
                + "Students must complete assignments and exams independently, properly cite sources, and avoid plagiarism or unauthorized assistance.";
    }

    private String defaultAttendancePolicy() {
        return "Attendance is compulsory. Students are normally required to achieve at least 70% attendance to be admitted to the final examination.";
    }

    private String defaultLateSubmissionPolicy() {
        return "Assignments are expected to be submitted on time. Late submissions may be penalized according to course and university policies.";
    }

    private String defaultExamPolicy() {
        return "Students who score less than the minimum threshold for attestation periods may fail the course according to university regulations.";
    }

    private String defaultClassroomPolicy() {
        return "Students must participate respectfully, avoid disrupting classes, and use laptops or mobile devices only for classroom purposes.";
    }

    private String defaultInclusionStatements() {
        return "Students are expected to listen actively, respect others, participate to the fullest of their ability, and support an inclusive learning environment.";
    }

    private void registerFonts(PdfRendererBuilder builder) {
        registerFont(builder, "DejaVu Serif",
                "C:\\Windows\\Fonts\\times.ttf",
                "C:\\Windows\\Fonts\\DejaVuSerif.ttf",
                "/usr/share/fonts/truetype/dejavu/DejaVuSerif.ttf",
                "/usr/share/fonts/dejavu/DejaVuSerif.ttf"
        );
        registerFont(builder, "DejaVu Sans",
                "C:\\Windows\\Fonts\\arial.ttf",
                "C:\\Windows\\Fonts\\DejaVuSans.ttf",
                "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
                "/usr/share/fonts/dejavu/DejaVuSans.ttf"
        );
    }

    private void registerFont(PdfRendererBuilder builder, String family, String... candidates) {
        for (var candidate : candidates) {
            var path = Path.of(candidate);
            if (Files.isRegularFile(path)) {
                builder.useFont(new File(candidate), family);
                return;
            }
        }
    }

    private static String loadOfficialLogo() {
        try (var input = SyllabusPdfExportService.class.getResourceAsStream("/pdf/aitu-logo.svg")) {
            if (input == null) {
                throw new IllegalStateException("Official AITU logo resource is missing");
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load official AITU logo resource", exception);
        }
    }
}
