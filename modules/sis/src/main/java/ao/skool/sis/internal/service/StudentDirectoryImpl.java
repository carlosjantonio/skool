package ao.skool.sis.internal.service;

import ao.skool.sis.api.StudentDirectory;
import ao.skool.sis.internal.domain.Student;
import ao.skool.sis.internal.persistence.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class StudentDirectoryImpl implements StudentDirectory {

    private final StudentRepository students;

    public StudentDirectoryImpl(StudentRepository students) {
        this.students = students;
    }

    @Override
    public Optional<StudentSummary> findStudent(UUID studentId) {
        return students.findById(studentId).map(this::toSummary);
    }

    @Override
    public Optional<StudentSummary> findStudentByUserId(UUID userId) {
        return students.findByUserId(userId).map(this::toSummary);
    }

    private StudentSummary toSummary(Student s) {
        return new StudentSummary(s.id(), s.tenantId(), s.fullName(),
                s.dateOfBirth(), s.sex().name(), s.userId());
    }
}
