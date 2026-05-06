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

    @Column(length = 255, nullable = false)
    private String fullName;

    @Column(nullable = false)
    private int courseNumber;

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

    public int getCourseNumber() {
        return courseNumber;
    }

    public void setCourseNumber(int courseNumber) {
        this.courseNumber = courseNumber;
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
