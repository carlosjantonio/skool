package ao.skool.academic_structure.internal.domain;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "trimesters",
       uniqueConstraints = @UniqueConstraint(name = "uk_trimesters_year_key", columnNames = {"academic_year_id", "trimester_key"}))
public class Trimester {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "academic_year_id", nullable = false)
    private UUID academicYearId;

    @Enumerated(EnumType.STRING)
    @Column(name = "trimester_key", nullable = false, length = 4)
    private TrimesterKey key;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    protected Trimester() {}

    public Trimester(UUID id, UUID academicYearId, TrimesterKey key, LocalDate startDate, LocalDate endDate) {
        this.id = id;
        this.academicYearId = academicYearId;
        this.key = key;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public UUID id() { return id; }
    public UUID academicYearId() { return academicYearId; }
    public TrimesterKey key() { return key; }
    public LocalDate startDate() { return startDate; }
    public LocalDate endDate() { return endDate; }
}
