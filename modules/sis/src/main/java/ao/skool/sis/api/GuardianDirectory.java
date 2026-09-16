package ao.skool.sis.api;

import java.util.List;
import java.util.UUID;

/**
 * Cross-module lookup for guardian ↔ student relationships. Used by fees to
 * scope invoice queries to a guardian's children.
 */
public interface GuardianDirectory {

    /** Ids of every student linked to the guardian identified by {@code userId}. */
    List<UUID> childrenOf(UUID userId);
}
