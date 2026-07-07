package ao.skool.identity.internal.service;

import ao.skool.common.domain.TenantId;
import ao.skool.common.security.Roles;
import ao.skool.identity.api.IdentityUserService;
import ao.skool.identity.internal.domain.User;
import ao.skool.identity.internal.persistence.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class IdentityUserServiceImpl implements IdentityUserService {

    private static final Logger log = LoggerFactory.getLogger(IdentityUserServiceImpl.class);
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    public IdentityUserServiceImpl(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UUID createGuardianUser(TenantId tenantId, String email, String fullName) {
        return createUser(tenantId, email, fullName, Set.of(Roles.GUARDIAN));
    }

    @Override
    public UUID createTeacherUser(TenantId tenantId, String email, String fullName) {
        return createUser(tenantId, email, fullName, Set.of(Roles.TEACHER));
    }

    @Override
    public UUID createUser(TenantId tenantId, String email, String fullName, Set<String> roles) {
        users.findByEmailIgnoreCase(email).ifPresent(u -> {
            throw new UserAlreadyExistsException(email);
        });
        String tempPassword = randomTempPassword();
        UUID userId = UUID.randomUUID();
        User user = new User(userId, tenantId.value(), email,
                passwordEncoder.encode(tempPassword), fullName, roles);
        users.save(user);
        // Phase 2 stub: log the temp password. In Phase 6 the notification module
        // will consume UserRegistered and dispatch it via email/SMS.
        log.info("Created user {} ({}). Temporary password: {}", email, roles, tempPassword);
        return userId;
    }

    private String randomTempPassword() {
        byte[] bytes = new byte[9];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
