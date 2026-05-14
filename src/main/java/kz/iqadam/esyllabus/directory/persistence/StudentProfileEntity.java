package kz.iqadam.esyllabus.directory.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "student_profiles")
public class StudentProfileEntity {

    @Id
    @Column(length = 64, nullable = false)
    private String id;

    @Column(length = 128, nullable = false, unique = true)
    private String username;

    @Column(length = 255, nullable = false)
    private String fullName;

    @Column(length = 255, nullable = false)
    private String email;

    @Column(nullable = false)
    private int courseNumber;

    @Column(length = 64)
    private String schoolId;

    @Column(length = 255)
    private String programName;

    @Column(length = 64)
    private String departmentId;

    @Column(length = 255)
    private String departmentName;

    @Column(length = 64, nullable = false)
    private String groupName;

    @Column(length = 2000)
    private String currentCourseIdsCsv;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getCourseNumber() {
        return courseNumber;
    }

    public void setCourseNumber(int courseNumber) {
        this.courseNumber = courseNumber;
    }

    public String getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(String schoolId) {
        this.schoolId = schoolId;
    }

    public String getProgramName() {
        return programName;
    }

    public void setProgramName(String programName) {
        this.programName = programName;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getCurrentCourseIdsCsv() {
        return currentCourseIdsCsv;
    }

    public void setCurrentCourseIdsCsv(String currentCourseIdsCsv) {
        this.currentCourseIdsCsv = currentCourseIdsCsv;
    }
}
