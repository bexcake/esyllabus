package kz.iqadam.esyllabus.integration.digital;

import com.fasterxml.jackson.databind.JsonNode;

public interface DigitalUniversityBridgeClient {

    JsonNode getStudentByEmail(String email);

    JsonNode getStudents(Integer course, Integer schoolId, Integer programId, int page, int size);

    JsonNode getEmployees();

    JsonNode getEmployees(Integer schoolId, int page, int size);

    JsonNode getEmployee(Integer employeeId);

    JsonNode getSchools();

    JsonNode getSchools(Integer schoolId);

    JsonNode getEducationPrograms();

    JsonNode getEducationPrograms(Integer programId);

    JsonNode getTeacherDisciplines();

    JsonNode getTeacherDisciplines(Integer schoolId, Integer teacherId, Integer academicYear, Integer term, int page, int size);
}
