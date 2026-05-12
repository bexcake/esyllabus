package kz.iqadam.esyllabus.directory.model;

public enum StaffRole {
    TEACHER,
    LECTURER,
    SCHOOL_DIRECTOR,
    LIBRARIAN;

    public boolean isTeachingStaff() {
        return this == TEACHER || this == LECTURER;
    }

    public String apiValue() {
        return isTeachingStaff() ? TEACHER.name() : name();
    }
}
