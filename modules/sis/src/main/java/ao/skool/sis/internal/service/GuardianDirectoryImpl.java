package ao.skool.sis.internal.service;

import ao.skool.sis.api.GuardianDirectory;
import ao.skool.sis.internal.domain.Guardian;
import ao.skool.sis.internal.domain.StudentGuardianLink;
import ao.skool.sis.internal.persistence.GuardianRepository;
import ao.skool.sis.internal.persistence.StudentGuardianRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GuardianDirectoryImpl implements GuardianDirectory {

    private final GuardianRepository guardians;
    private final StudentGuardianRepository links;

    public GuardianDirectoryImpl(GuardianRepository guardians, StudentGuardianRepository links) {
        this.guardians = guardians;
        this.links = links;
    }

    @Override
    public List<UUID> childrenOf(UUID userId) {
        Guardian guardian = guardians.findByUserId(userId).orElse(null);
        if (guardian == null) return List.of();
        return links.findByKeyGuardianId(guardian.id()).stream()
                .map(StudentGuardianLink::studentId).toList();
    }
}
