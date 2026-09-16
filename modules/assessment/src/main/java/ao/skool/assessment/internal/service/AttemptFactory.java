package ao.skool.assessment.internal.service;

import ao.skool.assessment.internal.domain.QuizAttempt;
import ao.skool.assessment.internal.persistence.QuizAttemptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Companion service whose sole job is to run the initial attempt insert in its own
 * transaction. Two concurrent "start attempt" requests race on the
 * {@code uk_attempts_quiz_student} constraint — in React's StrictMode dev double-fire,
 * or on any legitimate simultaneous tab open. If the save throws a constraint
 * violation here, only THIS transaction rolls back; the caller's outer transaction
 * stays clean and can re-fetch the winning row via a normal query.
 */
@Service
public class AttemptFactory {

    private final QuizAttemptRepository attempts;

    public AttemptFactory(QuizAttemptRepository attempts) {
        this.attempts = attempts;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public QuizAttempt insertFresh(QuizAttempt attempt) {
        return attempts.saveAndFlush(attempt);
    }
}
