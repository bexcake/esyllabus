package kz.iqadam.esyllabus.syllabus.api;

public record CourseCatalogItemResponse(
        String id,
        String title,
        String code,
        String program,
        String degreeLevel,
        String academicYear,
        String trimester,
        String languageOfInstruction,
        int credits,
        String status,
        java.util.List<String> instructors
) {
}
