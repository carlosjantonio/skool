package ao.skool.sis.internal.domain;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "student_guardians")
public class StudentGuardianLink {

    @EmbeddedId
    private Key key;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private GuardianRelationship relationship;

    @Column(name = "is_primary", nullable = false)
    private boolean primaryGuardian;

    @Column(name = "is_emergency_contact", nullable = false)
    private boolean emergencyContact;

    protected StudentGuardianLink() {}

    public StudentGuardianLink(UUID studentId, UUID guardianId, GuardianRelationship relationship,
                               boolean primaryGuardian, boolean emergencyContact) {
        this.key = new Key(studentId, guardianId);
        this.relationship = relationship;
        this.primaryGuardian = primaryGuardian;
        this.emergencyContact = emergencyContact;
    }

    public UUID studentId() { return key.studentId; }
    public UUID guardianId() { return key.guardianId; }
    public GuardianRelationship relationship() { return relationship; }
    public boolean primaryGuardian() { return primaryGuardian; }
    public boolean emergencyContact() { return emergencyContact; }

    @Embeddable
    public static class Key implements Serializable {
        @Column(name = "student_id", nullable = false)
        private UUID studentId;
        @Column(name = "guardian_id", nullable = false)
        private UUID guardianId;

        protected Key() {}
        public Key(UUID studentId, UUID guardianId) {
            this.studentId = studentId;
            this.guardianId = guardianId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key k)) return false;
            return Objects.equals(studentId, k.studentId) && Objects.equals(guardianId, k.guardianId);
        }
        @Override
        public int hashCode() { return Objects.hash(studentId, guardianId); }
    }
}
