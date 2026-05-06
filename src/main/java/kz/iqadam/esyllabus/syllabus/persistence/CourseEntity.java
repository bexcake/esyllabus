package kz.iqadam.esyllabus.syllabus.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "courses")
public class CourseEntity {

    @Id
    @Column(length = 128, nullable = false)
    private String id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false, length = 255)
    private String program;

    @Column(length = 128)
    private String schoolId;

    @Column(nullable = false, length = 32)
    private String degreeLevel;

    @Column(nullable = false, length = 32)
    private String academicYear;

    @Column(nullable = false, length = 32)
    private String trimester;

    @Column(nullable = false, length = 32)
    private String languageOfInstruction;

    @Column(nullable = false)
    private int credits;

    @Column(nullable = false, length = 2000)
    private String instructorsCsv;

    @Column(length = 2000)
    private String disciplineTagsCsv;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getProgram() {
        return program;
    }

    public void setProgram(String program) {
        this.program = program;
    }

    public String getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(String schoolId) {
        this.schoolId = schoolId;
    }

    public String getDegreeLevel() {
        return degreeLevel;
    }

    public void setDegreeLevel(String degreeLevel) {
        this.degreeLevel = degreeLevel;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
    }

    public String getTrimester() {
        return trimester;
    }

    public void setTrimester(String trimester) {
        this.trimester = trimester;
    }

    public String getLanguageOfInstruction() {
        return languageOfInstruction;
    }

    public void setLanguageOfInstruction(String languageOfInstruction) {
        this.languageOfInstruction = languageOfInstruction;
    }

    public int getCredits() {
        return credits;
    }

    public void setCredits(int credits) {
        this.credits = credits;
    }

    public String getInstructorsCsv() {
        return instructorsCsv;
    }

    public void setInstructorsCsv(String instructorsCsv) {
        this.instructorsCsv = instructorsCsv;
    }

    public String getDisciplineTagsCsv() {
        return disciplineTagsCsv;
    }

    public void setDisciplineTagsCsv(String disciplineTagsCsv) {
        this.disciplineTagsCsv = disciplineTagsCsv;
    }
}
