package ao.skool.assessment.internal.service;

import ao.skool.assessment.api.event.QuizPublished;
import ao.skool.assessment.internal.domain.Question;
import ao.skool.assessment.internal.domain.QuestionType;
import ao.skool.assessment.internal.domain.Quiz;
import ao.skool.assessment.internal.domain.QuizQuestion;
import ao.skool.assessment.internal.domain.QuizStatus;
import ao.skool.assessment.internal.persistence.QuestionRepository;
import ao.skool.assessment.internal.persistence.QuizQuestionRepository;
import ao.skool.assessment.internal.persistence.QuizRepository;
import ao.skool.assessment.internal.web.dto.AssessmentDtos.CreateQuiz;
import ao.skool.assessment.internal.web.dto.AssessmentDtos.QuizResponse;
import ao.skool.common.domain.SkoolPrincipal;
import ao.skool.common.domain.tenant.TenantContext;
import ao.skool.common.web.error.ApplicationException;
import ao.skool.common.web.error.NotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class QuizService {

    private final QuizRepository quizzes;
    private final QuizQuestionRepository quizQuestions;
    private final QuestionRepository questions;
    private final TenantContext tenant;
    private final ApplicationEventPublisher events;

    public QuizService(QuizRepository quizzes, QuizQuestionRepository quizQuestions,
                       QuestionRepository questions, TenantContext tenant,
                       ApplicationEventPublisher events) {
        this.quizzes = quizzes;
        this.quizQuestions = quizQuestions;
        this.questions = questions;
        this.tenant = tenant;
        this.events = events;
    }

    public QuizResponse create(CreateQuiz cmd) {
        var tenantId = tenant.current().value();

        // Verify every question belongs to this tenant to prevent cross-tenant leakage.
        var loaded = questions.findAllById(cmd.questionIds());
        if (loaded.size() != cmd.questionIds().size()) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, "error.validation");
        }
        for (Question q : loaded) {
            if (!q.tenantId().equals(tenantId)) {
                throw new ApplicationException(HttpStatus.BAD_REQUEST, "error.validation");
            }
        }

        Quiz quiz = new Quiz(UUID.randomUUID(), tenantId, cmd.subjectId(), cmd.turmaId(),
                cmd.academicYearId(), cmd.trimesterKey(), cmd.title(), cmd.instructions(),
                cmd.timeLimitSeconds(),
                cmd.randomizeQuestions() == null ? true : cmd.randomizeQuestions(),
                cmd.randomizeOptions() == null ? true : cmd.randomizeOptions(),
                cmd.opensAt(), cmd.closesAt(), currentActor());
        quizzes.save(quiz);

        int pos = 0;
        for (UUID qid : cmd.questionIds()) {
            quizQuestions.save(new QuizQuestion(quiz.id(), qid, pos++));
        }
        return toResponse(quiz, cmd.questionIds().size());
    }

    public QuizResponse publish(UUID quizId) {
        Quiz quiz = quizzes.findById(quizId).orElseThrow(NotFoundException::new);
        assertOwnedByCurrentTenant(quiz);
        if (quiz.status() != QuizStatus.DRAFT) {
            throw new ApplicationException(HttpStatus.CONFLICT, "error.conflict");
        }
        int count = quizQuestions.findByQuizIdOrderByPositionAsc(quizId).size();
        if (count == 0) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, "error.validation");
        }
        quiz.publish();
        events.publishEvent(new QuizPublished(UUID.randomUUID(), tenant.current(),
                quiz.id(), quiz.subjectId(), quiz.turmaId(), quiz.title(), Instant.now()));
        return toResponse(quiz, count);
    }

    public QuizResponse close(UUID quizId) {
        Quiz quiz = quizzes.findById(quizId).orElseThrow(NotFoundException::new);
        assertOwnedByCurrentTenant(quiz);
        quiz.close();
        int count = quizQuestions.findByQuizIdOrderByPositionAsc(quizId).size();
        return toResponse(quiz, count);
    }

    @Transactional(readOnly = true)
    public QuizResponse get(UUID quizId) {
        Quiz quiz = quizzes.findById(quizId).orElseThrow(NotFoundException::new);
        assertOwnedByCurrentTenant(quiz);
        int count = quizQuestions.findByQuizIdOrderByPositionAsc(quizId).size();
        return toResponse(quiz, count);
    }

    @Transactional(readOnly = true)
    public List<QuizResponse> listByTurma(UUID turmaId) {
        var tenantId = tenant.current().value();
        return quizzes.findByTenantIdAndTurmaIdOrderByCreatedAtDesc(tenantId, turmaId).stream()
                .map(q -> toResponse(q, quizQuestions.findByQuizIdOrderByPositionAsc(q.id()).size()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<QuizResponse> listAvailable(UUID turmaId) {
        return quizzes.findByTurmaIdAndStatusOrderByCreatedAtDesc(turmaId, QuizStatus.PUBLISHED).stream()
                .map(q -> toResponse(q, quizQuestions.findByQuizIdOrderByPositionAsc(q.id()).size()))
                .toList();
    }

    private void assertOwnedByCurrentTenant(Quiz quiz) {
        if (!quiz.tenantId().equals(tenant.current().value())) throw new NotFoundException();
    }

    private QuizResponse toResponse(Quiz q, int questionCount) {
        return new QuizResponse(q.id(), q.subjectId(), q.turmaId(), q.academicYearId(),
                q.trimesterKey(), q.title(), q.instructions(), q.timeLimitSeconds(),
                q.randomizeQuestions(), q.randomizeOptions(), q.status(),
                q.opensAt(), q.closesAt(), q.publishedAt(), questionCount);
    }

    private UUID currentActor() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof SkoolPrincipal p) {
            try { return UUID.fromString(p.userId()); } catch (IllegalArgumentException ignored) { return null; }
        }
        return null;
    }
}
