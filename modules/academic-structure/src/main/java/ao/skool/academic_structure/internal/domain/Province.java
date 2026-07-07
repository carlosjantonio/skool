package ao.skool.academic_structure.internal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Angolan province reference data. Keyed by short code (3 chars) since province names change
 * rarely and codes are easier to reference from other tables.
 */
@Entity
@Table(name = "provinces")
public class Province {

    @Id
    @Column(length = 3, nullable = false, updatable = false)
    private String code;

    @Column(nullable = false, length = 64)
    private String name;

    protected Province() {}

    public Province(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String code() { return code; }
    public String name() { return name; }
}
