package kz.iqadam.esyllabus.integration.digital;

import java.util.Set;

public interface DigitalUniversityRoleClient {

    Set<String> getRolesByEmail(String email);
}
