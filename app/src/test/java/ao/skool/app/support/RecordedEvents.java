package ao.skool.app.support;

import ao.skool.common.domain.event.DomainEvent;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.event.EventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Collects every {@link DomainEvent} published during a test, so a test can assert
 * "this event fired, exactly once, with these fields" without standing up a real
 * consumer. The notification module (Phase 6) will be the first real consumer; until
 * then this is the only thing proving the events are actually emitted.
 * <p>
 * The listener is plain {@code @EventListener}, not {@code @TransactionalEventListener},
 * matching how the publishers fire — so an event raised inside a MockMvc request is
 * visible the moment that request returns.
 */
@TestConfiguration
public class RecordedEvents {

    private final List<DomainEvent> recorded = Collections.synchronizedList(new ArrayList<>());

    @EventListener
    public void capture(DomainEvent event) {
        recorded.add(event);
    }

    public void clear() {
        recorded.clear();
    }

    public <T extends DomainEvent> List<T> ofType(Class<T> type) {
        synchronized (recorded) {
            return recorded.stream().filter(type::isInstance).map(type::cast).toList();
        }
    }

    public <T extends DomainEvent> T onlyOne(Class<T> type) {
        List<T> matches = ofType(type);
        if (matches.size() != 1) {
            throw new AssertionError("Expected exactly 1 " + type.getSimpleName()
                    + " but got " + matches.size() + ": " + matches);
        }
        return matches.getFirst();
    }

    public List<DomainEvent> all() {
        synchronized (recorded) {
            return List.copyOf(recorded);
        }
    }
}
