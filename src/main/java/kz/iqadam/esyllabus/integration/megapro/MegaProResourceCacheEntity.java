package kz.iqadam.esyllabus.integration.megapro;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "megapro_cache")
public class MegaProResourceCacheEntity {

    @Id
    @Column(length = 64, nullable = false)
    private String id;

    @Column(length = 128, nullable = false)
    private String courseId;

    @Column(length = 255, nullable = false)
    private String discipline;

    @Column(length = 2000)
    private String disciplineTagsCsv;

    @Column(length = 128)
    private String externalId;

    @Column(length = 255, nullable = false)
    private String title;

    @Column(length = 255)
    private String author;

    @Column(name = "publication_year", length = 16)
    private String publicationYear;

    @Column(length = 1000)
    private String url;

    @Column(length = 64)
    private String type;

    @Column(nullable = false)
    private Instant syncedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public String getDiscipline() {
        return discipline;
    }

    public void setDiscipline(String discipline) {
        this.discipline = discipline;
    }

    public String getDisciplineTagsCsv() {
        return disciplineTagsCsv;
    }

    public void setDisciplineTagsCsv(String disciplineTagsCsv) {
        this.disciplineTagsCsv = disciplineTagsCsv;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getPublicationYear() {
        return publicationYear;
    }

    public void setPublicationYear(String publicationYear) {
        this.publicationYear = publicationYear;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Instant getSyncedAt() {
        return syncedAt;
    }

    public void setSyncedAt(Instant syncedAt) {
        this.syncedAt = syncedAt;
    }
}
