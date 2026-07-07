package ao.skool.academic_structure.internal.domain;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "municipios",
       uniqueConstraints = @UniqueConstraint(name = "uk_municipios_prov_name", columnNames = {"provincia_code", "name"}))
public class Municipio {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "provincia_code", nullable = false, length = 3)
    private String provinciaCode;

    @Column(nullable = false, length = 64)
    private String name;

    protected Municipio() {}

    public Municipio(UUID id, String provinciaCode, String name) {
        this.id = id;
        this.provinciaCode = provinciaCode;
        this.name = name;
    }

    public UUID id() { return id; }
    public String provinciaCode() { return provinciaCode; }
    public String name() { return name; }
}
