package kz.iqadam.esyllabus.directory.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import kz.iqadam.esyllabus.directory.model.StaffRole;

@Entity
@Table(name = "staff_profiles")
public class StaffProfileEntity {

    @Id
    @Column(length = 64, nullable = false)
    private String id;

    @Column(length = 128, nullable = false, unique = true)
    private String username;

    @Column(length = 255, nullable = false)
    private String fullName;

    @Column(length = 255, nullable = false)
    private String email;

    @Column(length = 255)
    private String workplace;

    @Column(length = 128)
    private String cabinet;

    @Column(length = 255)
    private String positionTitle;

    @Column(length = 64, nullable = false)
    private String schoolId;

    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    private StaffRole role;

    @Column(unique = true)
    private Long duEmployeeId;

    @Column(unique = true)
    private Long duUserId;

    @Lob
    @Column
    private String duRawJson;

    @Column
    private Instant duSyncedAt;

    @Column
    private Instant duCacheExpiresAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getWorkplace() {
        return workplace;
    }

    public void setWorkplace(String workplace) {
        this.workplace = workplace;
    }

    public String getCabinet() {
        return cabinet;
    }

    public void setCabinet(String cabinet) {
        this.cabinet = cabinet;
    }

    public String getPositionTitle() {
        return positionTitle;
    }

    public void setPositionTitle(String positionTitle) {
        this.positionTitle = positionTitle;
    }

    public String getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(String schoolId) {
        this.schoolId = schoolId;
    }

    public StaffRole getRole() {
        return role;
    }

    public void setRole(StaffRole role) {
        this.role = role;
    }

    public Long getDuEmployeeId() {
        return duEmployeeId;
    }

    public void setDuEmployeeId(Long duEmployeeId) {
        this.duEmployeeId = duEmployeeId;
    }

    public Long getDuUserId() {
        return duUserId;
    }

    public void setDuUserId(Long duUserId) {
        this.duUserId = duUserId;
    }

    public String getDuRawJson() {
        return duRawJson;
    }

    public void setDuRawJson(String duRawJson) {
        this.duRawJson = duRawJson;
    }

    public Instant getDuSyncedAt() {
        return duSyncedAt;
    }

    public void setDuSyncedAt(Instant duSyncedAt) {
        this.duSyncedAt = duSyncedAt;
    }

    public Instant getDuCacheExpiresAt() {
        return duCacheExpiresAt;
    }

    public void setDuCacheExpiresAt(Instant duCacheExpiresAt) {
        this.duCacheExpiresAt = duCacheExpiresAt;
    }
}
