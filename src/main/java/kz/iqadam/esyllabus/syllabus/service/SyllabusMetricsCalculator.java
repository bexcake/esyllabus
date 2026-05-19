package kz.iqadam.esyllabus.syllabus.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SyllabusMetricsCalculator {

    private static final int CORE_SECTIONS_TOTAL = 17;

    public SyllabusMetrics calculate(JsonNode content) {
        var coreCompletion = new ArrayList<Boolean>();
        var missingSections = new ArrayList<SyllabusMetrics.MissingSection>();

        addCheck(
                coreCompletion,
                missingSections,
                hasText(content, "title")
                        && hasText(content, "code")
                        && hasText(content, "degreeLevel")
                        && hasText(content, "program")
                        && hasText(content, "academicYear")
                        && hasText(content, "trimester")
                        && hasText(content, "languageOfInstruction"),
                "core-info",
                "Basic course information",
                "Fill title, code, degree level, program, academic year, trimester, and language of instruction."
        );
        addCheck(
                coreCompletion,
                missingSections,
                arrayHasAny(content.path("instructors")),
                "instructors",
                "Instructors",
                "Add at least one instructor."
        );
        addCheck(
                coreCompletion,
                missingSections,
                content.path("credits").asInt(0) > 0 && workloadIsComplete(content.path("workload")),
                "credits-workload",
                "Credits and workload",
                "Set credits and make sure workload hours add up to totalHours."
        );
        addCheck(
                coreCompletion,
                missingSections,
                hasText(content, "prerequisites") && hasText(content, "postrequisites"),
                "prerequisites-postrequisites",
                "Prerequisites and postrequisites",
                "Fill both prerequisites and postrequisites."
        );
        addCheck(
                coreCompletion,
                missingSections,
                hasMeaningfulText(content.path("overview").asText("")),
                "overview",
                "Course overview",
                "Fill the overview section."
        );
        addCheck(
                coreCompletion,
                missingSections,
                stringArrayHasAny(content.path("goals")),
                "goals",
                "Course goals",
                "Add at least one goal."
        );
        addCheck(
                coreCompletion,
                missingSections,
                stringArrayHasAny(content.path("learningOutcomes")),
                "learning-outcomes",
                "Learning outcomes",
                "Add at least one learning outcome."
        );
        addCheck(
                coreCompletion,
                missingSections,
                stringArrayHasAny(content.path("teachingMethods")),
                "teaching-methods",
                "Teaching methods",
                "Add at least one teaching method."
        );
        addCheck(
                coreCompletion,
                missingSections,
                gradingIsComplete(content.path("gradingSchema")),
                "grading-schema",
                "Grading schema",
                "Add grading items with name and weight."
        );
        addCheck(
                coreCompletion,
                missingSections,
                hasMeaningfulText(content.path("academicIntegrity").asText("")),
                "academic-integrity",
                "Academic integrity",
                "Fill the academic integrity section."
        );
        addCheck(
                coreCompletion,
                missingSections,
                coursePoliciesComplete(content.path("coursePolicies")),
                "course-policies",
                "Course policies",
                "Fill attendance, late submissions, exams attestation, classroom behavior, communication policy, and other policies."
        );
        addCheck(
                coreCompletion,
                missingSections,
                hasMeaningfulText(content.path("inclusionStatements").asText("")),
                "inclusion-statements",
                "Inclusion statements",
                "Fill the inclusion statements section."
        );
        addCheck(
                coreCompletion,
                missingSections,
                stringArrayHasAny(content.path("technologyEmployed")),
                "technology-employed",
                "Technology employed",
                "Add at least one technology or platform."
        );
        addCheck(
                coreCompletion,
                missingSections,
                resourcesComplete(content.path("resources")),
                "resources",
                "Resources",
                "Add at least one resource with a title."
        );
        addCheck(
                coreCompletion,
                missingSections,
                abbreviationsComplete(content.path("abbreviations")),
                "abbreviations",
                "Abbreviations",
                "Add at least one abbreviation with short form and meaning."
        );
        addCheck(
                coreCompletion,
                missingSections,
                weeklyPlanComplete(content.path("weeklyPlan")),
                "weekly-plan",
                "Weekly plan",
                "Add at least one weekly plan item with a topic."
        );
        addCheck(
                coreCompletion,
                missingSections,
                detailedPlanComplete(content.path("detailedPlan")),
                "detailed-plan",
                "Detailed plan",
                "Add at least one detailed plan item with lecture topics."
        );

        var additionalCompletion = additionalCompletion(content.path("optionalSections"))
                + additionalCompletion(content.path("customSections"));
        var sectionsCompleted = (int) coreCompletion.stream().filter(Boolean.TRUE::equals).count() + additionalCompletion;
        var sectionsTotal = CORE_SECTIONS_TOTAL
                + safeArraySize(content.path("optionalSections"))
                + safeArraySize(content.path("customSections"));

        var progress = sectionsTotal == 0 ? 0 : Math.toIntExact(Math.round((sectionsCompleted * 100.0) / sectionsTotal));
        return new SyllabusMetrics(progress, sectionsCompleted, sectionsTotal, List.copyOf(missingSections));
    }

    private void addCheck(
            List<Boolean> coreCompletion,
            List<SyllabusMetrics.MissingSection> missingSections,
            boolean complete,
            String key,
            String label,
            String hint
    ) {
        coreCompletion.add(complete);
        if (!complete) {
            missingSections.add(new SyllabusMetrics.MissingSection(key, label, hint));
        }
    }

    private int additionalCompletion(JsonNode sections) {
        if (!sections.isArray()) {
            return 0;
        }

        var completed = 0;
        for (var section : sections) {
            if (additionalSectionComplete(section)) {
                completed++;
            }
        }
        return completed;
    }

    private boolean additionalSectionComplete(JsonNode section) {
        if (!hasText(section, "title")) {
            return false;
        }

        var content = section.path("content");
        var kind = content.path("kind").asText("");

        return switch (kind) {
            case "richText" -> hasMeaningfulText(content.path("html").asText(""));
            case "links" -> linksComplete(content.path("items"));
            case "table" -> tableHasRows(content.path("rows"));
            case "list" -> stringArrayHasAny(content.path("items"));
            case "structured" -> hasText(content, "intro") || structuredHasBlocks(content.path("blocks"));
            default -> false;
        };
    }

    private boolean hasText(JsonNode node, String field) {
        return node.path(field).asText("").trim().length() > 0;
    }

    private boolean hasMeaningfulText(String value) {
        return value.replaceAll("<[^>]+>", "").trim().length() > 0;
    }

    private boolean workloadIsComplete(JsonNode workload) {
        var lectures = workload.path("lecturesHours").asInt(0);
        var practice = workload.path("practiceHours").asInt(0);
        var lab = workload.path("labHours").asInt(0);
        var iass = workload.path("iassHours").asInt(0);
        var sis = workload.path("sisHours").asInt(0);
        var total = workload.path("totalHours").asInt(0);
        return lectures >= 0
                && practice >= 0
                && lab >= 0
                && iass >= 0
                && sis >= 0
                && total == lectures + practice + lab + iass + sis;
    }

    private boolean arrayHasAny(JsonNode array) {
        return array != null && array.isArray() && !array.isEmpty();
    }

    private boolean stringArrayHasAny(JsonNode array) {
        if (!arrayHasAny(array)) {
            return false;
        }
        for (var item : array) {
            if (item.asText("").trim().length() > 0) {
                return true;
            }
        }
        return false;
    }

    private boolean gradingIsComplete(JsonNode gradingSchema) {
        if (!arrayHasAny(gradingSchema)) {
            return false;
        }
        for (var item : gradingSchema) {
            if (item.path("name").asText("").trim().isBlank() || item.path("weight").asInt(0) <= 0) {
                return false;
            }
        }
        return true;
    }

    private boolean coursePoliciesComplete(JsonNode policies) {
        if (!policies.isObject()) {
            return false;
        }

        var fields = List.of(
                "attendance",
                "lateSubmissions",
                "examsAttestation",
                "classroomBehavior",
                "communicationPolicy",
                "otherPolicies"
        );

        return fields.stream().allMatch(field -> policies.path(field).asText("").trim().length() > 0);
    }

    private boolean resourcesComplete(JsonNode resources) {
        if (!arrayHasAny(resources)) {
            return false;
        }

        for (var resource : resources) {
            if (resource.path("title").asText("").trim().isBlank()) {
                return false;
            }
        }
        return true;
    }

    private boolean abbreviationsComplete(JsonNode abbreviations) {
        if (!arrayHasAny(abbreviations)) {
            return false;
        }

        for (var item : abbreviations) {
            if (item.path("shortForm").asText("").trim().isBlank()
                    || item.path("meaning").asText("").trim().isBlank()) {
                return false;
            }
        }
        return true;
    }

    private boolean weeklyPlanComplete(JsonNode weeklyPlan) {
        if (!arrayHasAny(weeklyPlan)) {
            return false;
        }
        for (var item : weeklyPlan) {
            if (item.path("topic").asText("").trim().isBlank()) {
                return false;
            }
        }
        return true;
    }

    private boolean detailedPlanComplete(JsonNode detailedPlan) {
        if (!arrayHasAny(detailedPlan)) {
            return false;
        }
        for (var item : detailedPlan) {
            if (item.path("lectureTopics").asText("").trim().isBlank()) {
                return false;
            }
        }
        return true;
    }

    private boolean linksComplete(JsonNode items) {
        if (!items.isArray()) {
            return false;
        }
        for (var item : items) {
            if (!item.path("label").asText("").trim().isBlank() && !item.path("url").asText("").trim().isBlank()) {
                return true;
            }
        }
        return false;
    }

    private boolean tableHasRows(JsonNode rows) {
        if (!rows.isArray()) {
            return false;
        }
        for (var row : rows) {
            if (row.isArray()) {
                for (var cell : row) {
                    if (!cell.asText("").trim().isBlank()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean structuredHasBlocks(JsonNode blocks) {
        if (!blocks.isArray()) {
            return false;
        }
        for (var block : blocks) {
            if (!block.path("heading").asText("").trim().isBlank()
                    || !block.path("body").asText("").trim().isBlank()) {
                return true;
            }
        }
        return false;
    }

    private int safeArraySize(JsonNode node) {
        return node.isArray() ? node.size() : 0;
    }
}
