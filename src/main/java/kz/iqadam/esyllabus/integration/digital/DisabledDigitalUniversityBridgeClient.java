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
    public JsonNode getEmployees() {
        return EMPTY_ARRAY;
    }

    @Override
    public JsonNode getEmployee(String employeeId) {
        return EMPTY_OBJECT;
    }

    @Override
    public JsonNode getSchools() {
        return EMPTY_ARRAY;
    }

    @Override
    public JsonNode getEducationPrograms() {
        return EMPTY_ARRAY;
    }

    @Override
    public JsonNode getTeacherDisciplines() {
        return EMPTY_ARRAY;
    }
}
