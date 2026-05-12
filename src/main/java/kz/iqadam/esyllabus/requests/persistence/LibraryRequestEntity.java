package kz.iqadam.esyllabus.requests.persistence;

import java.time.Instant;
import java.time.LocalDate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import kz.iqadam.esyllabus.requests.model.LibraryRequestStatus;

@Entity
@Table(name = "library_requests")
public class LibraryRequestEntity {

    @Id
    @Column(length = 64, nullable = false)
    private String id;

    @Column(length = 128, nullable = false)
    private String requesterUsername;

    @Column(length = 64)
    private String syllabusId;

    @Column(length = 255, nullable = false)
    private String requesterName;

    @Column(length = 64, nullable = false)
    private String schoolId;

    @Column(length = 255, nullable = false)
    private String schoolName;

    @Column(length = 128, nullable = false)
    private String directorUsername;

    @Column(length = 255, nullable = false)
    private String department;

    @Column(length = 128, nullable = false)
    private String educationLevel;

    @Column(nullable = false)
    private LocalDate requestDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 48, nullable = false)
    private LibraryRequestStatus status;

    @Column(length = 1000)
    private String directorComment;

    @Column(length = 1000)
    private String libraryFeedback;

    @Column(length = 16)
    private String expectedPurchaseMonth;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        var now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRequesterUsername() {
        return requesterUsername;
    }

    public void setRequesterUsername(String requesterUsername) {
        this.requesterUsername = requesterUsername;
    }

    public String getSyllabusId() {
        return syllabusId;
    }

    public void setSyllabusId(String syllabusId) {
        this.syllabusId = syllabusId;
    }

    public String getRequesterName() {
        return requesterName;
    }

    public void setRequesterName(String requesterName) {
        this.requesterName = requesterName;
    }

    public String getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(String schoolId) {
        this.schoolId = schoolId;
    }

    public String getSchoolName() {
        return schoolName;
    }

    public void setSchoolName(String schoolName) {
        this.schoolName = schoolName;
    }

    public String getDirectorUsername() {
        return directorUsername;
    }

    public void setDirectorUsername(String directorUsername) {
        this.directorUsername = directorUsername;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getEducationLevel() {
        return educationLevel;
    }

    public void setEducationLevel(String educationLevel) {
        this.educationLevel = educationLevel;
    }

    public LocalDate getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(LocalDate requestDate) {
        this.requestDate = requestDate;
    }

    public LibraryRequestStatus getStatus() {
        return status;
    }

    public void setStatus(LibraryRequestStatus status) {
        this.status = status;
    }

    public String getDirectorComment() {
        return directorComment;
    }

    public void setDirectorComment(String directorComment) {
        this.directorComment = directorComment;
    }

    public String getLibraryFeedback() {
        return libraryFeedback;
    }

    public void setLibraryFeedback(String libraryFeedback) {
        this.libraryFeedback = libraryFeedback;
    }

    public String getExpectedPurchaseMonth() {
        return expectedPurchaseMonth;
    }

    public void setExpectedPurchaseMonth(String expectedPurchaseMonth) {
        this.expectedPurchaseMonth = expectedPurchaseMonth;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
