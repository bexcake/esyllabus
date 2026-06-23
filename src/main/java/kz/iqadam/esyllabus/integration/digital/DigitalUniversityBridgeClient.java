package kz.iqadam.esyllabus.integration.digital;

import com.fasterxml.jackson.databind.JsonNode;

public interface DigitalUniversityBridgeClient {

    JsonNode getStudentByEmail(String email);

    JsonNode getStudentByEmail(String email, String bearerToken);

    JsonNode getStudents(Integer course, Integer schoolId, Integer programId, int page, int size);

    JsonNode getStudents(Integer course, Integer schoolId, Integer programId, int page, int size, String bearerToken);

    JsonNode getEmployees();

    JsonNode getEmployees(Integer schoolId, int page, int size);

    JsonNode getEmployees(Integer schoolId, int page, int size, String bearerToken);

    JsonNode getEmployee(Integer employeeId);

    JsonNode getEmployee(Integer employeeId, String bearerToken);

    JsonNode getSchools();

    JsonNode getSchools(Integer schoolId);

    JsonNode getSchools(Integer schoolId, String bearerToken);

    JsonNode getEducationPrograms();

    JsonNode getEducationPrograms(Integer programId);

    JsonNode getEducationPrograms(Integer programId, String bearerToken);

    JsonNode getTeacherDisciplines();

    JsonNode getTeacherDisciplines(Integer schoolId, Integer teacherId, Integer academicYear, Integer term, int page, int size);

    JsonNode getTeacherDisciplines(Integer schoolId, Integer teacherId, Integer academicYear, Integer term, int page, int size, String bearerToken);
}
