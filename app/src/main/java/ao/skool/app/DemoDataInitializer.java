package ao.skool.app;

import ao.skool.common.security.Roles;
import ao.skool.identity.internal.domain.User;
import ao.skool.identity.internal.persistence.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

/**
 * Seeds a demo admin bound to the demo school (id 00000000-0000-0000-0000-000000000001)
 * inserted by the academic-structure migration V2_1. Runs once at startup when
 * {@code skool.demo.seed=true} (default) and no admin exists yet.
 * <p>
 * The seeder deliberately lives in the app module because it composes state from two
 * modules — identity (users) and academic-structure (the school). Module code stays
 * decoupled.
 */
@Component
@ConditionalOnProperty(name = "skool.demo.seed", havingValue = "true", matchIfMissing = true)
public class DemoDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);
    private static final UUID DEMO_SCHOOL_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String demoEmail;
    private final String demoPassword;

    public DemoDataInitializer(UserRepository users,
                               PasswordEncoder passwordEncoder,
                               @Value("${skool.demo.admin-email:admin@skool.demo}") String demoEmail,
                               @Value("${skool.demo.admin-password:admin123}") String demoPassword) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.demoEmail = demoEmail;
        this.demoPassword = demoPassword;
    }

    @Override
    public void run(String... args) {
        if (users.findByEmailIgnoreCase(demoEmail).isPresent()) {
            return;
        }
        User admin = new User(
                UUID.randomUUID(),
                DEMO_SCHOOL_ID,
                demoEmail,
                passwordEncoder.encode(demoPassword),
                "Administrador Demo",
                Set.of(Roles.ADMIN)
        );
        users.save(admin);
        log.info("Seeded demo admin: {} / <configured password>", demoEmail);
    }
}
