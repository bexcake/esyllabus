package kz.iqadam.esyllabus.integration.digital.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "du_programs")
public class DigitalUniversityProgramEntity {

    @Id
    @Column(length = 64, nullable = false)
    private String id;

    @Column(unique = true)
    private Long externalProgramId;

    @Column(length = 128)
    private String code;

    @Column(length = 255, nullable = false)
    private String name;

    @Column(length = 64)
    private String schoolId;

    @Column(nullable = false)
    private boolean active;

    @Lob
    @Column
    private String rawJson;

    @Column(nullable = false)
    private Instant syncedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getExternalProgramId() {
        return externalProgramId;
    }

    public void setExternalProgramId(Long externalProgramId) {
        this.externalProgramId = externalProgramId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(String schoolId) {
        this.schoolId = schoolId;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getRawJson() {
        return rawJson;
    }

    public void setRawJson(String rawJson) {
        this.rawJson = rawJson;
    }

    public Instant getSyncedAt() {
        return syncedAt;
    }

    public void setSyncedAt(Instant syncedAt) {
        this.syncedAt = syncedAt;
    }
}
