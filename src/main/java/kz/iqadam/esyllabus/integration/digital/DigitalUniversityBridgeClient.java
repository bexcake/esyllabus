package kz.iqadam.esyllabus.integration.digital;

import com.fasterxml.jackson.databind.JsonNode;

public interface DigitalUniversityBridgeClient {

    JsonNode getStudentByEmail(String email);

    JsonNode getEmployees();

    JsonNode getEmployee(String employeeId);

    JsonNode getSchools();

    JsonNode getEducationPrograms();

    JsonNode getTeacherDisciplines();
}
