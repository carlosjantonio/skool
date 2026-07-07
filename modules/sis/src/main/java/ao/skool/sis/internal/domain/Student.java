package ao.skool.sis.internal.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "students",
       indexes = {
           @Index(name = "idx_students_tenant", columnList = "tenant_id"),
           @Index(name = "idx_students_bi", columnList = "bi")
       })
public class Student {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    /** BI is optional at enrolment (some children don't have one yet). */
    @Column(length = 14)
    private String bi;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 1)
    private Sex sex;

    @Column(name = "comuna_ou_bairro", length = 128)
    private String comunaOuBairro;

    @Column(name = "address_line1", length = 255)
    private String addressLine1;

    @Column(name = "health_notes", columnDefinition = "TEXT")
    private String healthNotes;

    @Column(name = "photo_document_id")
    private UUID photoDocumentId;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Student() {}

    public Student(UUID id, UUID tenantId, String fullName, LocalDate dateOfBirth, Sex sex) {
        this.id = id;
        this.tenantId = tenantId;
        this.fullName = fullName;
        this.dateOfBirth = dateOfBirth;
        this.sex = sex;
    }

    public UUID id() { return id; }
    public UUID tenantId() { return tenantId; }
    public String bi() { return bi; }
    public String fullName() { return fullName; }
    public LocalDate dateOfBirth() { return dateOfBirth; }
    public Sex sex() { return sex; }
    public String comunaOuBairro() { return comunaOuBairro; }
    public String addressLine1() { return addressLine1; }
    public String healthNotes() { return healthNotes; }
    public UUID photoDocumentId() { return photoDocumentId; }
    public boolean active() { return active; }

    public void setBi(String bi) { this.bi = bi; }
    public void setAddress(String comunaOuBairro, String addressLine1) {
        this.comunaOuBairro = comunaOuBairro;
        this.addressLine1 = addressLine1;
    }
    public void setHealthNotes(String notes) { this.healthNotes = notes; }
    public void deactivate() { this.active = false; }
}
