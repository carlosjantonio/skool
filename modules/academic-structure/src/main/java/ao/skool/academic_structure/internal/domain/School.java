package ao.skool.academic_structure.internal.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * A school is a tenant. Its id doubles as the tenant_id used by all other modules.
 */
@Entity
@Table(name = "schools",
       uniqueConstraints = @UniqueConstraint(name = "uk_schools_code", columnNames = "code"))
public class School {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(nullable = false, length = 32)
    private String code;

    @Column(name = "municipio_id", nullable = false)
    private UUID municipioId;

    @Column(name = "comuna_ou_bairro", length = 128)
    private String comunaOuBairro;

    @Column(name = "address_line1", length = 255)
    private String addressLine1;

    @Column(name = "address_complement", length = 255)
    private String addressComplement;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected School() {}

    public School(UUID id, String name, String code, UUID municipioId,
                  String comunaOuBairro, String addressLine1, String addressComplement) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.municipioId = municipioId;
        this.comunaOuBairro = comunaOuBairro;
        this.addressLine1 = addressLine1;
        this.addressComplement = addressComplement;
    }

    public UUID id() { return id; }
    public UUID tenantId() { return id; } // school id IS the tenant id
    public String name() { return name; }
    public String code() { return code; }
    public UUID municipioId() { return municipioId; }
    public String comunaOuBairro() { return comunaOuBairro; }
    public String addressLine1() { return addressLine1; }
    public String addressComplement() { return addressComplement; }
    public boolean active() { return active; }

    public void rename(String newName) { this.name = newName; }
    public void deactivate() { this.active = false; }
}
