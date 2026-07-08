package ao.skool.assessment.internal.service;

import ao.skool.assessment.api.event.QuizSubmitted;
import ao.skool.assessment.internal.domain.AttemptStatus;
import ao.skool.assessment.internal.domain.Question;
import ao.skool.assessment.internal.domain.QuestionType;
import ao.skool.assessment.internal.domain.Quiz;
import ao.skool.assessment.internal.domain.QuizAnswer;
import ao.skool.assessment.internal.domain.QuizAttempt;
import ao.skool.assessment.internal.domain.QuizStatus;
import ao.skool.assessment.internal.persistence.QuestionRepository;
import ao.skool.assessment.internal.persistence.QuizAnswerRepository;
import ao.skool.assessment.internal.persistence.QuizAttemptRepository;
import ao.skool.assessment.internal.persistence.QuizQuestionRepository;
import ao.skool.assessment.internal.persistence.QuizRepository;
import ao.skool.assessment.internal.web.dto.AssessmentDtos.AttemptResult;
import ao.skool.assessment.internal.web.dto.AssessmentDtos.AttemptView;
import ao.skool.assessment.internal.web.dto.AssessmentDtos.ManualGradeBatch;
import ao.skool.assessment.internal.web.dto.AssessmentDtos.QuestionStat;
import ao.skool.assessment.internal.web.dto.AssessmentDtos.QuizAnalytics;
import ao.skool.assessment.internal.web.dto.AssessmentDtos.QuizAttemptRow;
import ao.skool.assessment.internal.web.dto.AssessmentDtos.SavedAnswer;
import ao.skool.assessment.internal.web.dto.AssessmentDtos.StudentQuestion;
import ao.skool.assessment.internal.web.dto.AssessmentDtos.SubmitAnswer;
import ao.skool.assessment.internal.web.dto.AssessmentDtos.SubmitAttempt;
import ao.skool.common.domain.tenant.TenantContext;
import ao.skool.common.web.error.ApplicationException;
import ao.skool.common.web.error.NotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class AttemptService {

    private final QuizRepository quizzes;
    private final QuizQuestionRepository quizQuestions;
    private final QuestionRepository questions;
    private final QuizAttemptRepository attempts;
    private final QuizAnswerRepository answers;
    private final TenantContext tenant;
    private final ApplicationEventPublisher events;
    private final ObjectMapper mapper;

    public AttemptService(QuizRepository quizzes, QuizQuestionRepository quizQuestions,
                          QuestionRepository questions, QuizAttemptRepository attempts,
                          QuizAnswerRepository answers, TenantContext tenant,
                          ApplicationEventPublisher events, ObjectMapper mapper) {
        this.quizzes = quizzes;
        this.quizQuestions = quizQuestions;
        this.questions = questions;
        this.attempts = attempts;
        this.answers = answers;
        this.tenant = tenant;
        this.events = events;
        this.mapper = mapper;
    }

    /**
     * Starts (or resumes) a student's attempt. Freezing the question order at start
     * prevents a reload from re-randomizing and lets the student's answers cache
     * offline against a stable question list.
     */
    public AttemptView startOrResume(UUID quizId, UUID studentId) {
        Quiz quiz = quizzes.findById(quizId).orElseThrow(NotFoundException::new);
        if (!quiz.tenantId().equals(tenant.current().value())) throw new NotFoundException();
        if (quiz.status() != QuizStatus.PUBLISHED) {
            throw new ApplicationException(HttpStatus.CONFLICT, "error.conflict");
        }
        Instant now = Instant.now();
        if (quiz.opensAt() != null && now.isBefore(quiz.opensAt())) {
            throw new ApplicationException(HttpStatus.CONFLICT, "error.conflict");
        }
        if (quiz.closesAt() != null && now.isAfter(quiz.closesAt())) {
            throw new ApplicationException(HttpStatus.CONFLICT, "error.conflict");
        }

        QuizAttempt attempt = attempts.findByQuizIdAndStudentId(quizId, studentId).orElse(null);
        List<UUID> orderedIds;
        if (attempt == null) {
            List<UUID> ids = new ArrayList<>();
            for (var qq : quizQuestions.findByQuizIdOrderByPositionAsc(quizId)) ids.add(qq.questionId());
            if (quiz.randomizeQuestions()) {
                Random rng = new Random(seedFor(quizId, studentId));
                Collections.shuffle(ids, rng);
            }
            orderedIds = ids;
            String orderJson = writeJson(mapper.valueToTree(ids));
            attempt = new QuizAttempt(UUID.randomUUID(), tenant.current().value(),
                    quizId, studentId, orderJson);
            attempts.save(attempt);
        } else {
            orderedIds = readOrder(attempt.questionOrder());
        }

        Map<UUID, Question> byId = new HashMap<>();
        for (Question q : questions.findAllById(orderedIds)) byId.put(q.id(), q);

        List<StudentQuestion> shown = new ArrayList<>();
        for (UUID qid : orderedIds) {
            Question q = byId.get(qid);
            if (q == null) continue;
            JsonNode payload = maskAndMaybeShuffle(q, quiz.randomizeOptions(), studentId);
            shown.add(new StudentQuestion(q.id(), q.prompt(), q.questionType(), payload, q.points()));
        }

        List<SavedAnswer> saved = new ArrayList<>();
        for (QuizAnswer a : answers.findByAttemptId(attempt.id())) {
            saved.add(new SavedAnswer(a.questionId(), readJson(a.response())));
        }

        Instant deadline = null;
        if (quiz.timeLimitSeconds() != null) {
            deadline = attempt.startedAt().plusSeconds(quiz.timeLimitSeconds());
        }
        if (quiz.closesAt() != null && (deadline == null || quiz.closesAt().isBefore(deadline))) {
            deadline = quiz.closesAt();
        }

        return new AttemptView(attempt.id(), quiz.id(), quiz.title(), quiz.instructions(),
                quiz.timeLimitSeconds(), attempt.startedAt(), deadline, attempt.status(), shown, saved);
    }

    /**
     * Persists answers and (if final) auto-grades objective questions. Idempotent by
     * client-supplied answer id — replaying the same batch after a network blip
     * updates the same rows instead of duplicating them.
     */
    public AttemptResult submit(UUID attemptId, UUID studentId, SubmitAttempt cmd, boolean finalSubmit) {
        QuizAttempt attempt = attempts.findById(attemptId).orElseThrow(NotFoundException::new);
        if (!attempt.studentId().equals(studentId)) throw new NotFoundException();
        if (!attempt.tenantId().equals(tenant.current().value())) throw new NotFoundException();
        if (attempt.status() != AttemptStatus.IN_PROGRESS && finalSubmit) {
            // Idempotent final submit — return the frozen result rather than re-grading.
            return snapshot(attempt);
        }

        Quiz quiz = quizzes.findById(attempt.quizId()).orElseThrow(NotFoundException::new);

        // Deadline enforcement: if the student's clock or the network held them past
        // the wall-clock deadline, we still accept the answers but the attempt gets
        // finalized regardless of the finalSubmit flag.
        Instant now = Instant.now();
        boolean pastDeadline = false;
        if (quiz.timeLimitSeconds() != null) {
            Instant deadline = attempt.startedAt().plusSeconds(quiz.timeLimitSeconds());
            if (now.isAfter(deadline)) pastDeadline = true;
        }
        if (quiz.closesAt() != null && now.isAfter(quiz.closesAt())) pastDeadline = true;

        // Persist / upsert each answer.
        for (SubmitAnswer sa : cmd.answers()) {
            var existing = answers.findById(sa.id()).orElse(null);
            String respJson = writeJson(sa.response());
            if (existing != null) {
                existing.updateResponse(respJson);
            } else {
                // Guard against a rogue client sending an id that already belongs to a
                // different (attempt, question) pair — surface as validation error.
                var byPair = answers.findByAttemptIdAndQuestionId(attemptId, sa.questionId());
                if (byPair.isPresent()) {
                    byPair.get().updateResponse(respJson);
                    continue;
                }
                answers.save(new QuizAnswer(sa.id(), attemptId, sa.questionId(), respJson));
            }
        }

        if (cmd.tabSwitchCount() != null && cmd.tabSwitchCount() > attempt.tabSwitchCount()) {
            for (int i = attempt.tabSwitchCount(); i < cmd.tabSwitchCount(); i++) {
                attempt.incrementTabSwitch();
            }
        }

        if (!finalSubmit && !pastDeadline) {
            return snapshot(attempt);
        }
        return finalize(attempt);
    }

    private AttemptResult finalize(QuizAttempt attempt) {
        List<UUID> orderedIds = readOrder(attempt.questionOrder());
        Map<UUID, Question> byId = new HashMap<>();
        for (Question q : questions.findAllById(orderedIds)) byId.put(q.id(), q);

        BigDecimal auto = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        boolean needsManual = false;
        for (UUID qid : orderedIds) {
            Question q = byId.get(qid);
            if (q == null) continue;
            total = total.add(q.points());
            QuizAnswer ans = answers.findByAttemptIdAndQuestionId(attempt.id(), qid).orElse(null);
            if (ans == null) continue;
            if (q.questionType() == QuestionType.ESSAY) {
                needsManual = true;
                continue;
            }
            BigDecimal earned = gradeObjective(q, ans);
            boolean correct = earned.compareTo(q.points()) == 0;
            ans.applyAutoGrade(correct, earned);
            if (correct) auto = auto.add(earned);
        }
        attempt.markSubmitted(auto, total, needsManual);

        events.publishEvent(new QuizSubmitted(UUID.randomUUID(), tenant.current(),
                attempt.id(), attempt.quizId(), attempt.studentId(),
                auto, total, needsManual, Instant.now()));

        return new AttemptResult(attempt.id(), attempt.status(), auto, attempt.manualScore(),
                total, needsManual);
    }

    private BigDecimal gradeObjective(Question q, QuizAnswer ans) {
        JsonNode key = readJson(q.payload());
        JsonNode resp = readJson(ans.response());
        return switch (q.questionType()) {
            case MULTIPLE_CHOICE -> gradeMultipleChoice(key, resp, q.points());
            case TRUE_FALSE      -> gradeTrueFalse(key, resp, q.points());
            case SHORT_ANSWER    -> gradeShortAnswer(key, resp, q.points());
            case ESSAY           -> BigDecimal.ZERO;
        };
    }

    private BigDecimal gradeMultipleChoice(JsonNode key, JsonNode resp, BigDecimal points) {
        Set<String> correct = new HashSet<>();
        JsonNode opts = key.path("options");
        if (opts.isArray()) {
            for (JsonNode o : opts) {
                if (o.path("correct").asBoolean(false)) correct.add(o.path("key").asText());
            }
        }
        Set<String> selected = new HashSet<>();
        JsonNode sel = resp.path("selectedKeys");
        if (sel.isArray()) for (JsonNode s : sel) selected.add(s.asText());
        return correct.equals(selected) && !correct.isEmpty() ? points : BigDecimal.ZERO;
    }

    private BigDecimal gradeTrueFalse(JsonNode key, JsonNode resp, BigDecimal points) {
        boolean correct = key.path("correct").asBoolean(false);
        boolean chosen = resp.path("answer").asBoolean(false);
        return correct == chosen ? points : BigDecimal.ZERO;
    }

    private BigDecimal gradeShortAnswer(JsonNode key, JsonNode resp, BigDecimal points) {
        JsonNode acc = key.path("acceptedAnswers");
        String given = resp.path("text").asText("").trim().toLowerCase(Locale.ROOT);
        if (given.isEmpty()) return BigDecimal.ZERO;
        if (acc.isArray()) {
            for (JsonNode a : acc) {
                if (a.asText("").trim().toLowerCase(Locale.ROOT).equals(given)) return points;
            }
        }
        return BigDecimal.ZERO;
    }

    /**
     * Removes correctness flags from the payload sent to students and shuffles
     * multiple-choice option ordering per-student (seed = studentId + questionId).
     */
    private JsonNode maskAndMaybeShuffle(Question q, boolean randomize, UUID studentId) {
        JsonNode raw = readJson(q.payload());
        ObjectNode masked = mapper.createObjectNode();
        switch (q.questionType()) {
            case MULTIPLE_CHOICE -> {
                ArrayNode outOpts = mapper.createArrayNode();
                List<ObjectNode> opts = new ArrayList<>();
                for (JsonNode o : raw.path("options")) {
                    ObjectNode co = mapper.createObjectNode();
                    co.put("key", o.path("key").asText());
                    co.put("text", o.path("text").asText());
                    opts.add(co);
                }
                if (randomize) {
                    long seed = seedFor(studentId, q.id());
                    Collections.shuffle(opts, new Random(seed));
                }
                for (ObjectNode o : opts) outOpts.add(o);
                masked.set("options", outOpts);
            }
            case TRUE_FALSE, ESSAY, SHORT_ANSWER -> { /* no-op — no revealing data */ }
        }
        return masked;
    }

    @Transactional(readOnly = true)
    public List<QuizAttemptRow> attemptsForQuiz(UUID quizId) {
        return attempts.findByQuizIdOrderByStartedAtAsc(quizId).stream()
                .map(a -> new QuizAttemptRow(a.id(), a.studentId(), a.status(), a.startedAt(),
                        a.submittedAt(), a.autoScore(), a.manualScore(), a.totalPoints(),
                        a.tabSwitchCount()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<QuizAttemptRow> attemptsForStudent(UUID studentId) {
        return attempts.findByStudentIdOrderByStartedAtDesc(studentId).stream()
                .map(a -> new QuizAttemptRow(a.id(), a.studentId(), a.status(), a.startedAt(),
                        a.submittedAt(), a.autoScore(), a.manualScore(), a.totalPoints(),
                        a.tabSwitchCount()))
                .toList();
    }

    /**
     * Per-question correctness stats + submitted-attempt average. Only counts
     * SUBMITTED / GRADED attempts so an in-flight student doesn't skew the numbers.
     */
    @Transactional(readOnly = true)
    public QuizAnalytics analyticsFor(UUID quizId) {
        var quizAttempts = attempts.findByQuizIdOrderByStartedAtAsc(quizId);
        int total = quizAttempts.size();
        int submitted = 0;
        BigDecimal sumAuto = BigDecimal.ZERO;
        BigDecimal sumTotal = BigDecimal.ZERO;
        for (QuizAttempt a : quizAttempts) {
            if (a.status() == ao.skool.assessment.internal.domain.AttemptStatus.IN_PROGRESS) continue;
            submitted++;
            sumAuto = sumAuto.add(a.autoScore() == null ? BigDecimal.ZERO : a.autoScore());
            sumTotal = sumTotal.add(a.totalPoints() == null ? BigDecimal.ZERO : a.totalPoints());
        }
        BigDecimal avgAuto = submitted == 0 ? BigDecimal.ZERO
                : sumAuto.divide(BigDecimal.valueOf(submitted), 2, java.math.RoundingMode.HALF_UP);
        BigDecimal avgTotal = submitted == 0 ? BigDecimal.ZERO
                : sumTotal.divide(BigDecimal.valueOf(submitted), 2, java.math.RoundingMode.HALF_UP);

        List<UUID> qids = new ArrayList<>();
        for (var qq : quizQuestions.findByQuizIdOrderByPositionAsc(quizId)) qids.add(qq.questionId());
        Map<UUID, Question> qById = new HashMap<>();
        for (Question q : questions.findAllById(qids)) qById.put(q.id(), q);

        List<QuestionStat> stats = new ArrayList<>();
        for (UUID qid : qids) {
            int answered = 0;
            int correct = 0;
            for (QuizAttempt a : quizAttempts) {
                if (a.status() == ao.skool.assessment.internal.domain.AttemptStatus.IN_PROGRESS) continue;
                var ans = answers.findByAttemptIdAndQuestionId(a.id(), qid).orElse(null);
                if (ans == null) continue;
                answered++;
                if (Boolean.TRUE.equals(ans.isCorrect())) correct++;
            }
            BigDecimal rate = answered == 0 ? BigDecimal.ZERO
                    : BigDecimal.valueOf(correct)
                            .divide(BigDecimal.valueOf(answered), 4, java.math.RoundingMode.HALF_UP);
            Question q = qById.get(qid);
            stats.add(new QuestionStat(qid, q == null ? "?" : q.prompt(), answered, correct, rate));
        }
        return new QuizAnalytics(quizId, total, submitted, avgAuto, avgTotal, stats);
    }

    public void gradeEssayAnswers(UUID attemptId, ManualGradeBatch batch) {
        QuizAttempt attempt = attempts.findById(attemptId).orElseThrow(NotFoundException::new);
        if (!attempt.tenantId().equals(tenant.current().value())) throw new NotFoundException();

        BigDecimal manual = attempt.manualScore() == null ? BigDecimal.ZERO : attempt.manualScore();
        for (var g : batch.grades()) {
            QuizAnswer ans = answers.findById(g.answerId()).orElseThrow(NotFoundException::new);
            if (!ans.attemptId().equals(attemptId)) throw new NotFoundException();
            BigDecimal prev = ans.pointsEarned() == null ? BigDecimal.ZERO : ans.pointsEarned();
            ans.applyManualGrade(g.pointsEarned(), g.feedback());
            manual = manual.subtract(prev).add(g.pointsEarned());
        }
        attempt.applyManualScore(manual);
    }

    private AttemptResult snapshot(QuizAttempt a) {
        return new AttemptResult(a.id(), a.status(), a.autoScore(), a.manualScore(),
                a.totalPoints(), a.status() == AttemptStatus.SUBMITTED);
    }

    private long seedFor(UUID a, UUID b) {
        return a.getMostSignificantBits() ^ a.getLeastSignificantBits()
                ^ b.getMostSignificantBits() ^ b.getLeastSignificantBits();
    }

    private String writeJson(JsonNode node) {
        try { return mapper.writeValueAsString(node); }
        catch (JsonProcessingException e) { throw new IllegalArgumentException("invalid json", e); }
    }

    private JsonNode readJson(String raw) {
        try { return mapper.readTree(raw); }
        catch (JsonProcessingException e) { return mapper.createObjectNode(); }
    }

    private List<UUID> readOrder(String raw) {
        List<UUID> out = new ArrayList<>();
        JsonNode arr = readJson(raw);
        if (arr.isArray()) {
            for (JsonNode n : arr) {
                try { out.add(UUID.fromString(n.asText())); } catch (IllegalArgumentException ignored) {}
            }
        }
        return out;
    }
}
