package ao.skool.academic_structure.internal.domain;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "academic_years",
       uniqueConstraints = @UniqueConstraint(name = "uk_academic_years_tenant_name", columnNames = {"tenant_id", "name"}))
public class AcademicYear {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 32)
    private String name; // e.g. "2026" or "2026/2027"

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "is_current", nullable = false)
    private boolean current;

    @OneToMany(mappedBy = "academicYearId", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<Trimester> trimesters = new ArrayList<>();

    protected AcademicYear() {}

    public AcademicYear(UUID id, UUID tenantId, String name, LocalDate startDate, LocalDate endDate) {
        this.id = id;
        this.tenantId = tenantId;
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public UUID id() { return id; }
    public UUID tenantId() { return tenantId; }
    public String name() { return name; }
    public LocalDate startDate() { return startDate; }
    public LocalDate endDate() { return endDate; }
    public boolean current() { return current; }
    public List<Trimester> trimesters() { return List.copyOf(trimesters); }

    public void addTrimester(Trimester trimester) { this.trimesters.add(trimester); }
    public void markCurrent(boolean flag) { this.current = flag; }
}
