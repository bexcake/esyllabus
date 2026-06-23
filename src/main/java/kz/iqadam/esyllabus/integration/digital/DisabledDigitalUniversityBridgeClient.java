package kz.iqadam.esyllabus.integration.digital;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

final class DisabledDigitalUniversityBridgeClient implements DigitalUniversityBridgeClient {

    private static final JsonNode EMPTY_ARRAY = JsonNodeFactory.instance.arrayNode();
    private static final JsonNode EMPTY_OBJECT = JsonNodeFactory.instance.objectNode();

    @Override
    public JsonNode getStudentByEmail(String email) {
        return EMPTY_OBJECT;
    }

    @Override
    public JsonNode getStudents(Integer course, Integer schoolId, Integer programId, int page, int size) {
        return EMPTY_ARRAY;
    }

    @Override
    public JsonNode getEmployees() {
        return EMPTY_ARRAY;
    }

    @Override
    public JsonNode getEmployees(Integer schoolId, int page, int size) {
        return EMPTY_ARRAY;
    }

    @Override
    public JsonNode getEmployee(Integer employeeId) {
        return EMPTY_OBJECT;
    }

    @Override
    public JsonNode getSchools() {
        return EMPTY_ARRAY;
    }

    @Override
    public JsonNode getSchools(Integer schoolId) {
        return EMPTY_ARRAY;
    }

    @Override
    public JsonNode getEducationPrograms() {
        return EMPTY_ARRAY;
    }

    @Override
    public JsonNode getEducationPrograms(Integer programId) {
        return EMPTY_ARRAY;
    }

    @Override
    public JsonNode getTeacherDisciplines() {
        return EMPTY_ARRAY;
    }

    @Override
    public JsonNode getTeacherDisciplines(Integer schoolId, Integer teacherId, Integer academicYear, Integer term, int page, int size) {
        return EMPTY_ARRAY;
    }
}
