package ao.skool.identity.api;

import ao.skool.common.domain.TenantId;

import java.util.Set;
import java.util.UUID;

/**
 * Public identity API for other modules to provision users tied to their own aggregates
 * (guardians in SIS, teachers in staff). Implementation lives in identity.internal.
 * <p>
 * Never expose password fields on this interface — the identity module owns credential
 * lifecycle. A caller supplies enough profile info to create a login; identity handles
 * the initial random password + first-login reset flow.
 */
public interface IdentityUserService {

    /**
     * Create a portal user for a guardian.
     * @return the created user id (also usable as {@code SkoolPrincipal.userId})
     * @throws UserAlreadyExistsException if the email is already registered globally
     */
    UUID createGuardianUser(TenantId tenantId, String email, String fullName);

    /** Same for a teacher. */
    UUID createTeacherUser(TenantId tenantId, String email, String fullName);

    /** Generic escape hatch when the caller has explicit roles to assign. */
    UUID createUser(TenantId tenantId, String email, String fullName, Set<String> roles);

    class UserAlreadyExistsException extends RuntimeException {
        public UserAlreadyExistsException(String email) {
            super("User already exists: " + email);
        }
    }
}
