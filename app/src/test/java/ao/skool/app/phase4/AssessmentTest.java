package ao.skool.app.phase4;

import ao.skool.academic_structure.internal.web.dto.AcademicYearDtos.AcademicYearResponse;
import ao.skool.academic_structure.internal.web.dto.SubjectDtos.SubjectResponse;
import ao.skool.academic_structure.internal.web.dto.TurmaDtos.TurmaResponse;
import ao.skool.app.support.SchoolFixture;
import ao.skool.assessment.api.event.QuizPublished;
import ao.skool.assessment.api.event.QuizSubmitted;
import ao.skool.assessment.internal.domain.AttemptStatus;
import ao.skool.assessment.internal.domain.QuestionType;
import ao.skool.assessment.internal.domain.QuizStatus;
import ao.skool.assessment.internal.web.dto.AssessmentDtos.AttemptResult;
import ao.skool.assessment.internal.web.dto.AssessmentDtos.AttemptView;
import ao.skool.assessment.internal.web.dto.AssessmentDtos.CreateQuestion;
import ao.skool.assessment.internal.web.dto.AssessmentDtos.CreateQuiz;
import ao.skool.assessment.internal.web.dto.AssessmentDtos.ManualGradeBatch;
import ao.skool.assessment.internal.web.dto.AssessmentDtos.ManualGradeCommand;
import ao.skool.assessment.internal.web.dto.AssessmentDtos.QuestionResponse;
import ao.skool.assessment.internal.web.dto.AssessmentDtos.QuestionStat;
import ao.skool.assessment.internal.web.dto.AssessmentDtos.QuizAnalytics;
import ao.skool.assessment.internal.web.dto.AssessmentDtos.QuizAttemptRow;
import ao.skool.assessment.internal.web.dto.AssessmentDtos.QuizResponse;
import ao.skool.assessment.internal.web.dto.AssessmentDtos.StudentQuestion;
import ao.skool.assessment.internal.web.dto.AssessmentDtos.SubmitAnswer;
import ao.skool.assessment.internal.web.dto.AssessmentDtos.SubmitAttempt;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 4 — the assessment module: question bank, quiz lifecycle, offline-tolerant
 * attempts, auto-grading and analytics.
 * <p>
 * The exit criterion from the plan is a teacher building a timed quiz that a student
 * takes on a phone with the network cut mid-quiz; the answers must sync on reconnect.
 * On the server that reduces to two properties, both asserted below: the question order
 * is frozen at start so a reload cannot reshuffle it, and answer writes are idempotent
 * by client-supplied id so a replayed queue cannot duplicate or lose an answer.
 */
class AssessmentTest extends SchoolFixture {

    // ------------------------------------------------------------- fixtures

    private record Course(UUID subjectId, UUID turmaId, UUID yearId) {}

    private Course course(Actor admin) throws Exception {
        AcademicYearResponse year = createAcademicYear(admin);
        TurmaResponse turma = createTurma(admin, year.id());
        SubjectResponse subject = createSubject(admin, "Matemática");
        return new Course(subject.id(), turma.id(), year.id());
    }

    private ObjectNode multipleChoiceKey(String correctKey) {
        ObjectNode payload = json.createObjectNode();
        ArrayNode options = payload.putArray("options");
        for (String key : List.of("a", "b", "c", "d")) {
            ObjectNode option = options.addObject();
            option.put("key", key);
            option.put("text", "Opção " + key.toUpperCase());
            option.put("correct", key.equals(correctKey));
        }
        return payload;
    }

    private QuestionResponse question(Actor teacher, Course c, QuestionType type,
                                       String prompt, JsonNode payload, String points) throws Exception {
        return post(teacher, "/api/questions",
                new CreateQuestion(c.subjectId(), "CLASSE_10", prompt, type, payload, new BigDecimal(points)),
                QuestionResponse.class);
    }

    private QuestionResponse mcQuestion(Actor teacher, Course c, String prompt, String correctKey) throws Exception {
        return question(teacher, c, QuestionType.MULTIPLE_CHOICE, prompt, multipleChoiceKey(correctKey), "2.00");
    }

    private QuestionResponse trueFalseQuestion(Actor teacher, Course c, String prompt, boolean correct) throws Exception {
        ObjectNode payload = json.createObjectNode();
        payload.put("correct", correct);
        return question(teacher, c, QuestionType.TRUE_FALSE, prompt, payload, "1.00");
    }

    private QuestionResponse shortAnswerQuestion(Actor teacher, Course c, String prompt, String... accepted) throws Exception {
        ObjectNode payload = json.createObjectNode();
        ArrayNode list = payload.putArray("acceptedAnswers");
        for (String a : accepted) list.add(a);
        return question(teacher, c, QuestionType.SHORT_ANSWER, prompt, payload, "3.00");
    }

    private QuestionResponse essayQuestion(Actor teacher, Course c, String prompt) throws Exception {
        ObjectNode payload = json.createObjectNode();
        payload.put("rubric", "Clareza 50%, argumentação 50%");
        return question(teacher, c, QuestionType.ESSAY, prompt, payload, "4.00");
    }

    private QuizResponse createQuiz(Actor teacher, Course c, List<UUID> questionIds,
                                     Integer timeLimitSeconds, boolean randomize) throws Exception {
        return post(teacher, "/api/quizzes",
                new CreateQuiz(c.subjectId(), c.turmaId(), c.yearId(), "T1", "Teste de Matemática",
                        "Responda a todas as perguntas.", timeLimitSeconds, randomize, randomize,
                        null, null, questionIds),
                QuizResponse.class);
    }

    private QuizResponse publish(Actor teacher, UUID quizId) throws Exception {
        return post(teacher, "/api/quizzes/" + quizId + "/publish", null, QuizResponse.class);
    }

    private SubmitAnswer answer(UUID questionId, ObjectNode response) {
        // Client-generated id — the PWA derives a deterministic v5 UUID so a replayed
        // offline queue writes the same row twice rather than two rows.
        return new SubmitAnswer(UUID.randomUUID(), questionId, response);
    }

    private ObjectNode chooseOption(String key) {
        ObjectNode node = json.createObjectNode();
        node.putArray("selectedKeys").add(key);
        return node;
    }

    private ObjectNode answerBoolean(boolean value) {
        ObjectNode node = json.createObjectNode();
        node.put("answer", value);
        return node;
    }

    private ObjectNode answerText(String text) {
        ObjectNode node = json.createObjectNode();
        node.put("text", text);
        return node;
    }

    // ------------------------------------------------------- question bank

    @Test
    @DisplayName("the question bank stores all four question types, filtered by subject")
    void buildsQuestionBank() throws Exception {
        Actor teacher = teacher();
        Course c = course(admin());

        QuestionResponse mc = mcQuestion(teacher, c, "Quanto é 7 × 8?", "c");
        QuestionResponse tf = trueFalseQuestion(teacher, c, "Zero é um número par.", true);
        QuestionResponse sa = shortAnswerQuestion(teacher, c, "Capital de Angola?", "Luanda");
        QuestionResponse essay = essayQuestion(teacher, c, "Explique o teorema de Pitágoras.");

        assertThat(mc.questionType()).isEqualTo(QuestionType.MULTIPLE_CHOICE);
        assertThat(mc.points()).isEqualByComparingTo("2.00");
        assertThat(essay.points()).isEqualByComparingTo("4.00");

        List<QuestionResponse> bank = get(teacher,
                "/api/questions?subjectId=" + c.subjectId() + "&gradeLevel=CLASSE_10", new TypeReference<>() {});
        assertThat(bank).extracting(QuestionResponse::id)
                .contains(mc.id(), tf.id(), sa.id(), essay.id());

        // The bank is reusable across classes — it is keyed by subject + grade, not by quiz.
        assertThat(get(teacher, "/api/questions?subjectId=" + UUID.randomUUID(),
                new TypeReference<List<QuestionResponse>>() {})).isEmpty();
    }

    @Test
    @DisplayName("points default to 1 when the teacher does not weight the question")
    void questionPointsDefaultToOne() throws Exception {
        Actor teacher = teacher();
        Course c = course(admin());

        QuestionResponse q = post(teacher, "/api/questions",
                new CreateQuestion(c.subjectId(), "CLASSE_10", "Pergunta simples",
                        QuestionType.TRUE_FALSE, json.createObjectNode().put("correct", true), null),
                QuestionResponse.class);

        assertThat(q.points()).isEqualByComparingTo("1");
    }

    // ------------------------------------------------------- quiz lifecycle

    @Test
    @DisplayName("a quiz starts as a draft and only becomes visible to students once published")
    void quizLifecycle() throws Exception {
        Actor teacher = teacher();
        Course c = course(admin());
        QuestionResponse q = mcQuestion(teacher, c, "Quanto é 7 × 8?", "c");

        QuizResponse draft = createQuiz(teacher, c, List.of(q.id()), null, false);
        assertThat(draft.status()).isEqualTo(QuizStatus.DRAFT);
        assertThat(draft.questionCount()).isEqualTo(1);
        assertThat(draft.publishedAt()).isNull();

        events.clear();
        QuizResponse published = publish(teacher, draft.id());
        assertThat(published.status()).isEqualTo(QuizStatus.PUBLISHED);
        assertThat(published.publishedAt()).isNotNull();

        QuizPublished event = events.onlyOne(QuizPublished.class);
        assertThat(event.quizId()).isEqualTo(draft.id());
        assertThat(event.turmaId()).isEqualTo(c.turmaId());
        assertThat(event.title()).isEqualTo("Teste de Matemática");

        // Publishing twice is a conflict — the event must not fire again.
        assertThat(postStatus(teacher, "/api/quizzes/" + draft.id() + "/publish", null)).isEqualTo(409);
        assertThat(events.ofType(QuizPublished.class)).hasSize(1);
    }

    @Test
    @DisplayName("a quiz cannot be published with no questions")
    void cannotPublishEmptyQuiz() throws Exception {
        Actor teacher = teacher();
        Course c = course(admin());

        // @NotEmpty on questionIds stops an empty quiz being created in the first place.
        assertThat(postStatus(teacher, "/api/quizzes",
                new CreateQuiz(c.subjectId(), c.turmaId(), c.yearId(), "T1", "Vazio", null,
                        null, false, false, null, null, List.of()))).isEqualTo(400);
    }

    @Test
    @DisplayName("a quiz cannot be built from another school's questions")
    void rejectsCrossTenantQuestions() throws Exception {
        Actor teacher = teacher();
        Course c = course(admin());

        assertThat(postStatus(teacher, "/api/quizzes",
                new CreateQuiz(c.subjectId(), c.turmaId(), c.yearId(), "T1", "Roubado", null,
                        null, false, false, null, null, List.of(UUID.randomUUID()))))
                .isEqualTo(400);
    }

    @Test
    @DisplayName("students only see published quizzes for their turma")
    void studentsSeeOnlyPublishedQuizzes() throws Exception {
        Actor admin = admin();
        Actor teacher = teacher();
        Course c = course(admin);
        var student = enrolledStudent(admin, "João Baptista", c.yearId(), c.turmaId());

        QuestionResponse q = mcQuestion(teacher, c, "Quanto é 7 × 8?", "c");
        QuizResponse draft = createQuiz(teacher, c, List.of(q.id()), null, false);
        QuizResponse live = publish(teacher, createQuiz(teacher, c, List.of(q.id()), null, false).id());

        List<QuizResponse> available = get(student.login(),
                "/api/student/quizzes/available?turmaId=" + c.turmaId(), new TypeReference<>() {});

        assertThat(available).extracting(QuizResponse::id)
                .contains(live.id())
                .doesNotContain(draft.id());
    }

    @Test
    @DisplayName("a student cannot start an unpublished quiz")
    void cannotAttemptDraftQuiz() throws Exception {
        Actor admin = admin();
        Actor teacher = teacher();
        Course c = course(admin);
        var student = enrolledStudent(admin, "João Baptista", c.yearId(), c.turmaId());
        QuestionResponse q = mcQuestion(teacher, c, "Quanto é 7 × 8?", "c");
        QuizResponse draft = createQuiz(teacher, c, List.of(q.id()), null, false);

        assertThat(postStatus(student.login(), "/api/student/quizzes/" + draft.id() + "/attempts", null))
                .isEqualTo(409);
    }

    @Test
    @DisplayName("a closed quiz can no longer be started")
    void cannotAttemptClosedQuiz() throws Exception {
        Actor admin = admin();
        Actor teacher = teacher();
        Course c = course(admin);
        var student = enrolledStudent(admin, "João Baptista", c.yearId(), c.turmaId());
        QuestionResponse q = mcQuestion(teacher, c, "Quanto é 7 × 8?", "c");
        QuizResponse quiz = publish(teacher, createQuiz(teacher, c, List.of(q.id()), null, false).id());

        post(teacher, "/api/quizzes/" + quiz.id() + "/close", null, QuizResponse.class);

        assertThat(postStatus(student.login(), "/api/student/quizzes/" + quiz.id() + "/attempts", null))
                .isEqualTo(409);
    }

    // ------------------------------------------------------------- attempts

    @Test
    @DisplayName("a student attempt never reveals which option is correct")
    void attemptMasksTheAnswerKey() throws Exception {
        Actor admin = admin();
        Actor teacher = teacher();
        Course c = course(admin);
        var student = enrolledStudent(admin, "João Baptista", c.yearId(), c.turmaId());

        QuestionResponse mc = mcQuestion(teacher, c, "Quanto é 7 × 8?", "c");
        QuestionResponse tf = trueFalseQuestion(teacher, c, "Zero é par.", true);
        QuestionResponse sa = shortAnswerQuestion(teacher, c, "Capital de Angola?", "Luanda");
        QuizResponse quiz = publish(teacher, createQuiz(teacher, c, List.of(mc.id(), tf.id(), sa.id()), null, false).id());

        AttemptView view = post(student.login(), "/api/student/quizzes/" + quiz.id() + "/attempts",
                null, AttemptView.class);

        String rendered = json.writeValueAsString(view);
        assertThat(rendered)
                .as("no correctness flag, accepted answer or rubric may reach the student")
                .doesNotContain("\"correct\"")
                .doesNotContain("acceptedAnswers")
                .doesNotContain("Luanda")
                .doesNotContain("rubric");

        // Options are still there — they just carry key + text only.
        StudentQuestion shownMc = view.questions().stream()
                .filter(q -> q.id().equals(mc.id())).findFirst().orElseThrow();
        assertThat(shownMc.payload().get("options")).hasSize(4);
        assertThat(shownMc.payload().get("options").get(0).fieldNames())
                .toIterable().containsExactlyInAnyOrder("key", "text");
    }

    @Test
    @DisplayName("resuming returns the same attempt with the question order frozen")
    void resumeKeepsTheSameAttemptAndOrder() throws Exception {
        Actor admin = admin();
        Actor teacher = teacher();
        Course c = course(admin);
        var student = enrolledStudent(admin, "João Baptista", c.yearId(), c.turmaId());

        List<UUID> bank = List.of(
                mcQuestion(teacher, c, "P1", "a").id(),
                mcQuestion(teacher, c, "P2", "b").id(),
                mcQuestion(teacher, c, "P3", "c").id(),
                mcQuestion(teacher, c, "P4", "d").id(),
                mcQuestion(teacher, c, "P5", "a").id());
        // randomize ON — this is the case where a reshuffle on reload would be a disaster.
        QuizResponse quiz = publish(teacher, createQuiz(teacher, c, bank, null, true).id());

        AttemptView first = post(student.login(), "/api/student/quizzes/" + quiz.id() + "/attempts",
                null, AttemptView.class);
        AttemptView resumed = post(student.login(), "/api/student/quizzes/" + quiz.id() + "/attempts",
                null, AttemptView.class);

        assertThat(resumed.attemptId())
                .as("starting again must resume, never create a second attempt")
                .isEqualTo(first.attemptId());
        assertThat(resumed.questions()).extracting(StudentQuestion::id)
                .containsExactlyElementsOf(first.questions().stream().map(StudentQuestion::id).toList());
        assertThat(first.questions()).hasSize(5);
        assertThat(first.status()).isEqualTo(AttemptStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("two students get independently randomised orders from the same quiz")
    void randomisationIsPerStudent() throws Exception {
        Actor admin = admin();
        Actor teacher = teacher();
        Course c = course(admin);
        var a = enrolledStudent(admin, "Aluno A", c.yearId(), c.turmaId());
        var b = enrolledStudent(admin, "Aluno B", c.yearId(), c.turmaId());

        List<UUID> bank = new java.util.ArrayList<>();
        for (int i = 1; i <= 10; i++) bank.add(mcQuestion(teacher, c, "P" + i, "a").id());
        QuizResponse quiz = publish(teacher, createQuiz(teacher, c, bank, null, true).id());

        List<UUID> orderA = post(a.login(), "/api/student/quizzes/" + quiz.id() + "/attempts", null,
                AttemptView.class).questions().stream().map(StudentQuestion::id).toList();
        List<UUID> orderB = post(b.login(), "/api/student/quizzes/" + quiz.id() + "/attempts", null,
                AttemptView.class).questions().stream().map(StudentQuestion::id).toList();

        assertThat(orderA).containsExactlyInAnyOrderElementsOf(orderB);
        assertThat(orderA)
                .as("10 questions shuffled per-student — identical orders would mean the seed is not per-student")
                .isNotEqualTo(orderB);
    }

    @Test
    @DisplayName("a timed quiz reports the deadline derived from when the student started")
    void timedQuizExposesADeadline() throws Exception {
        Actor admin = admin();
        Actor teacher = teacher();
        Course c = course(admin);
        var student = enrolledStudent(admin, "João Baptista", c.yearId(), c.turmaId());
        QuestionResponse q = mcQuestion(teacher, c, "P1", "a");
        QuizResponse quiz = publish(teacher, createQuiz(teacher, c, List.of(q.id()), 1_800, false).id());

        AttemptView view = post(student.login(), "/api/student/quizzes/" + quiz.id() + "/attempts",
                null, AttemptView.class);

        assertThat(view.timeLimitSeconds()).isEqualTo(1_800);
        assertThat(view.deadlineAt()).isEqualTo(view.startedAt().plusSeconds(1_800));
    }

    @Test
    @DisplayName("saving a draft does not finalise the attempt, and the answers come back on resume")
    void draftSaveKeepsTheAttemptOpen() throws Exception {
        Actor admin = admin();
        Actor teacher = teacher();
        Course c = course(admin);
        var student = enrolledStudent(admin, "João Baptista", c.yearId(), c.turmaId());
        QuestionResponse q1 = mcQuestion(teacher, c, "P1", "a");
        QuestionResponse q2 = mcQuestion(teacher, c, "P2", "b");
        QuizResponse quiz = publish(teacher, createQuiz(teacher, c, List.of(q1.id(), q2.id()), null, false).id());

        AttemptView view = post(student.login(), "/api/student/quizzes/" + quiz.id() + "/attempts",
                null, AttemptView.class);

        AttemptResult saved = put(student.login(), "/api/student/quizzes/attempts/" + view.attemptId(),
                new SubmitAttempt(List.of(answer(q1.id(), chooseOption("a"))), 0), AttemptResult.class);

        assertThat(saved.status()).isEqualTo(AttemptStatus.IN_PROGRESS);
        assertThat(saved.autoScore()).as("nothing is graded until the student submits").isNull();

        // Coming back after losing the connection: the saved answer is still there.
        AttemptView resumed = post(student.login(), "/api/student/quizzes/" + quiz.id() + "/attempts",
                null, AttemptView.class);
        assertThat(resumed.savedAnswers()).singleElement().satisfies(a -> {
            assertThat(a.questionId()).isEqualTo(q1.id());
            assertThat(a.response().get("selectedKeys").get(0).asText()).isEqualTo("a");
        });
    }

    @Test
    @DisplayName("replaying the same answer id updates the row instead of adding one")
    void answerWritesAreIdempotent() throws Exception {
        Actor admin = admin();
        Actor teacher = teacher();
        Course c = course(admin);
        var student = enrolledStudent(admin, "João Baptista", c.yearId(), c.turmaId());
        QuestionResponse q = mcQuestion(teacher, c, "P1", "a");
        QuizResponse quiz = publish(teacher, createQuiz(teacher, c, List.of(q.id()), null, false).id());

        AttemptView view = post(student.login(), "/api/student/quizzes/" + quiz.id() + "/attempts",
                null, AttemptView.class);

        UUID stableAnswerId = UUID.randomUUID();
        SubmitAnswer first = new SubmitAnswer(stableAnswerId, q.id(), chooseOption("b"));
        SubmitAnswer corrected = new SubmitAnswer(stableAnswerId, q.id(), chooseOption("a"));

        put(student.login(), "/api/student/quizzes/attempts/" + view.attemptId(),
                new SubmitAttempt(List.of(first), 0), AttemptResult.class);
        // The offline queue flushes twice: once stale, once with the correction.
        put(student.login(), "/api/student/quizzes/attempts/" + view.attemptId(),
                new SubmitAttempt(List.of(first), 0), AttemptResult.class);
        put(student.login(), "/api/student/quizzes/attempts/" + view.attemptId(),
                new SubmitAttempt(List.of(corrected), 0), AttemptResult.class);

        AttemptView resumed = post(student.login(), "/api/student/quizzes/" + quiz.id() + "/attempts",
                null, AttemptView.class);
        assertThat(resumed.savedAnswers()).hasSize(1);
        assertThat(resumed.savedAnswers().getFirst().response().get("selectedKeys").get(0).asText())
                .isEqualTo("a");
    }

    @Test
    @DisplayName("a fresh answer id for a question already answered still updates that one answer")
    void aSecondIdForTheSameQuestionDoesNotDuplicate() throws Exception {
        Actor admin = admin();
        Actor teacher = teacher();
        Course c = course(admin);
        var student = enrolledStudent(admin, "João Baptista", c.yearId(), c.turmaId());
        QuestionResponse q = mcQuestion(teacher, c, "P1", "a");
        QuizResponse quiz = publish(teacher, createQuiz(teacher, c, List.of(q.id()), null, false).id());
        AttemptView view = post(student.login(), "/api/student/quizzes/" + quiz.id() + "/attempts",
                null, AttemptView.class);

        put(student.login(), "/api/student/quizzes/attempts/" + view.attemptId(),
                new SubmitAttempt(List.of(answer(q.id(), chooseOption("b"))), 0), AttemptResult.class);
        // A client that lost its local id and generated a new one for the same question.
        put(student.login(), "/api/student/quizzes/attempts/" + view.attemptId(),
                new SubmitAttempt(List.of(answer(q.id(), chooseOption("c"))), 0), AttemptResult.class);

        AttemptView resumed = post(student.login(), "/api/student/quizzes/" + quiz.id() + "/attempts",
                null, AttemptView.class);
        assertThat(resumed.savedAnswers()).hasSize(1);
        assertThat(resumed.savedAnswers().getFirst().response().get("selectedKeys").get(0).asText())
                .isEqualTo("c");
    }

    // -------------------------------------------------------- auto-grading

    @Test
    @DisplayName("objective questions are auto-graded on submit; essays are held for the teacher")
    void autoGradesObjectiveQuestions() throws Exception {
        Actor admin = admin();
        Actor teacher = teacher();
        Course c = course(admin);
        var student = enrolledStudent(admin, "João Baptista", c.yearId(), c.turmaId());

        QuestionResponse mcRight = mcQuestion(teacher, c, "Quanto é 7 × 8?", "c");        // 2 points
        QuestionResponse mcWrong = mcQuestion(teacher, c, "Quanto é 9 × 9?", "d");        // 2 points
        QuestionResponse tf = trueFalseQuestion(teacher, c, "Zero é par.", true);         // 1 point
        QuestionResponse sa = shortAnswerQuestion(teacher, c, "Capital?", "Luanda");      // 3 points
        QuestionResponse essay = essayQuestion(teacher, c, "Explique.");                  // 4 points

        QuizResponse quiz = publish(teacher, createQuiz(teacher, c,
                List.of(mcRight.id(), mcWrong.id(), tf.id(), sa.id(), essay.id()), null, false).id());

        AttemptView view = post(student.login(), "/api/student/quizzes/" + quiz.id() + "/attempts",
                null, AttemptView.class);

        events.clear();
        AttemptResult result = post(student.login(),
                "/api/student/quizzes/attempts/" + view.attemptId() + "/submit",
                new SubmitAttempt(List.of(
                        answer(mcRight.id(), chooseOption("c")),          // correct   → 2
                        answer(mcWrong.id(), chooseOption("a")),          // incorrect → 0
                        answer(tf.id(), answerBoolean(true)),             // correct   → 1
                        answer(sa.id(), answerText("  luanda  ")),        // correct, case/space tolerant → 3
                        answer(essay.id(), answerText("Uma resposta longa."))), 0),
                AttemptResult.class);

        assertThat(result.status()).isEqualTo(AttemptStatus.SUBMITTED);
        assertThat(result.autoScore()).isEqualByComparingTo("6.00");       // 2 + 1 + 3
        assertThat(result.totalPoints()).isEqualByComparingTo("12.00");    // 2+2+1+3+4
        assertThat(result.needsManualGrading()).isTrue();

        QuizSubmitted event = events.onlyOne(QuizSubmitted.class);
        assertThat(event.attemptId()).isEqualTo(view.attemptId());
        assertThat(event.autoScore()).isEqualByComparingTo("6.00");
        assertThat(event.needsManualGrading()).isTrue();
    }

    @Test
    @DisplayName("a quiz with no essay is fully graded and needs no teacher attention")
    void fullyObjectiveQuizNeedsNoManualGrading() throws Exception {
        Actor admin = admin();
        Actor teacher = teacher();
        Course c = course(admin);
        var student = enrolledStudent(admin, "João Baptista", c.yearId(), c.turmaId());

        QuestionResponse tf = trueFalseQuestion(teacher, c, "Zero é par.", true);
        QuizResponse quiz = publish(teacher, createQuiz(teacher, c, List.of(tf.id()), null, false).id());
        AttemptView view = post(student.login(), "/api/student/quizzes/" + quiz.id() + "/attempts",
                null, AttemptView.class);

        AttemptResult result = post(student.login(),
                "/api/student/quizzes/attempts/" + view.attemptId() + "/submit",
                new SubmitAttempt(List.of(answer(tf.id(), answerBoolean(false))), 0), AttemptResult.class);

        assertThat(result.needsManualGrading()).isFalse();
        // No essay means nothing is waiting on a human, so the attempt lands on GRADED
        // directly rather than sitting in SUBMITTED.
        assertThat(result.status()).isEqualTo(AttemptStatus.GRADED);
        assertThat(result.autoScore()).isEqualByComparingTo("0.00");
        assertThat(result.totalPoints()).isEqualByComparingTo("1.00");
    }

    @Test
    @DisplayName("an unanswered question scores nothing but still counts toward the total")
    void unansweredQuestionsStillCountTowardTheTotal() throws Exception {
        Actor admin = admin();
        Actor teacher = teacher();
        Course c = course(admin);
        var student = enrolledStudent(admin, "João Baptista", c.yearId(), c.turmaId());

        QuestionResponse answered = trueFalseQuestion(teacher, c, "P1", true);   // 1 point
        QuestionResponse skipped = mcQuestion(teacher, c, "P2", "a");            // 2 points
        QuizResponse quiz = publish(teacher, createQuiz(teacher, c,
                List.of(answered.id(), skipped.id()), null, false).id());
        AttemptView view = post(student.login(), "/api/student/quizzes/" + quiz.id() + "/attempts",
                null, AttemptView.class);

        AttemptResult result = post(student.login(),
                "/api/student/quizzes/attempts/" + view.attemptId() + "/submit",
                new SubmitAttempt(List.of(answer(answered.id(), answerBoolean(true))), 0),
                AttemptResult.class);

        assertThat(result.autoScore()).isEqualByComparingTo("1.00");
        assertThat(result.totalPoints()).isEqualByComparingTo("3.00");
    }

    @Test
    @DisplayName("submitting twice returns the frozen result instead of re-grading")
    void finalSubmitIsIdempotent() throws Exception {
        Actor admin = admin();
        Actor teacher = teacher();
        Course c = course(admin);
        var student = enrolledStudent(admin, "João Baptista", c.yearId(), c.turmaId());
        QuestionResponse tf = trueFalseQuestion(teacher, c, "Zero é par.", true);
        QuizResponse quiz = publish(teacher, createQuiz(teacher, c, List.of(tf.id()), null, false).id());
        AttemptView view = post(student.login(), "/api/student/quizzes/" + quiz.id() + "/attempts",
                null, AttemptView.class);

        AttemptResult first = post(student.login(),
                "/api/student/quizzes/attempts/" + view.attemptId() + "/submit",
                new SubmitAttempt(List.of(answer(tf.id(), answerBoolean(true))), 0), AttemptResult.class);

        events.clear();
        // The network dropped the response and the client retried — with a wrong answer
        // this time. The already-final attempt must not be re-opened or re-graded.
        AttemptResult replay = post(student.login(),
                "/api/student/quizzes/attempts/" + view.attemptId() + "/submit",
                new SubmitAttempt(List.of(answer(tf.id(), answerBoolean(false))), 0), AttemptResult.class);

        assertThat(replay.autoScore()).isEqualByComparingTo(first.autoScore());
        assertThat(replay.status()).isEqualTo(first.status()).isNotEqualTo(AttemptStatus.IN_PROGRESS);
        assertThat(events.ofType(QuizSubmitted.class))
                .as("a replayed submit must not fire a second QuizSubmitted").isEmpty();
    }

    @Test
    @DisplayName("answers arriving after the time limit finalise the attempt anyway")
    void lateAnswersAutoFinalise() throws Exception {
        Actor admin = admin();
        Actor teacher = teacher();
        Course c = course(admin);
        var student = enrolledStudent(admin, "João Baptista", c.yearId(), c.turmaId());
        QuestionResponse tf = trueFalseQuestion(teacher, c, "Zero é par.", true);

        // closesAt already in the past → any save is past the deadline.
        QuizResponse quiz = post(teacher, "/api/quizzes",
                new CreateQuiz(c.subjectId(), c.turmaId(), c.yearId(), "T1", "Teste expirado", null,
                        null, false, false, null, Instant.now().plus(2, ChronoUnit.SECONDS),
                        List.of(tf.id())), QuizResponse.class);
        publish(teacher, quiz.id());

        AttemptView view = post(student.login(), "/api/student/quizzes/" + quiz.id() + "/attempts",
                null, AttemptView.class);
        assertThat(view.deadlineAt()).isNotNull();

        Thread.sleep(2_500);

        // A *draft* save (finalSubmit = false) past the deadline must still close the attempt.
        AttemptResult result = put(student.login(), "/api/student/quizzes/attempts/" + view.attemptId(),
                new SubmitAttempt(List.of(answer(tf.id(), answerBoolean(true))), 0), AttemptResult.class);

        assertThat(result.status())
                .as("a draft save after the deadline auto-submits rather than leaving it open")
                .isNotEqualTo(AttemptStatus.IN_PROGRESS);
        assertThat(result.totalPoints()).isEqualByComparingTo("1.00");
    }

    @Test
    @DisplayName("tab switches are counted for the teacher to see")
    void recordsTabSwitches() throws Exception {
        Actor admin = admin();
        Actor teacher = teacher();
        Course c = course(admin);
        var student = enrolledStudent(admin, "João Baptista", c.yearId(), c.turmaId());
        QuestionResponse tf = trueFalseQuestion(teacher, c, "Zero é par.", true);
        QuizResponse quiz = publish(teacher, createQuiz(teacher, c, List.of(tf.id()), null, false).id());
        AttemptView view = post(student.login(), "/api/student/quizzes/" + quiz.id() + "/attempts",
                null, AttemptView.class);

        put(student.login(), "/api/student/quizzes/attempts/" + view.attemptId(),
                new SubmitAttempt(List.of(answer(tf.id(), answerBoolean(true))), 2), AttemptResult.class);
        post(student.login(), "/api/student/quizzes/attempts/" + view.attemptId() + "/submit",
                new SubmitAttempt(List.of(), 3), AttemptResult.class);

        List<QuizAttemptRow> rows = get(teacher, "/api/quizzes/" + quiz.id() + "/attempts",
                new TypeReference<>() {});
        assertThat(rows).singleElement()
                .extracting(QuizAttemptRow::tabSwitchCount).isEqualTo(3);
    }

    @Test
    @DisplayName("a student cannot submit into another student's attempt")
    void cannotSubmitIntoSomeoneElsesAttempt() throws Exception {
        Actor admin = admin();
        Actor teacher = teacher();
        Course c = course(admin);
        var victim = enrolledStudent(admin, "Aluno A", c.yearId(), c.turmaId());
        var attacker = enrolledStudent(admin, "Aluno B", c.yearId(), c.turmaId());

        QuestionResponse tf = trueFalseQuestion(teacher, c, "Zero é par.", true);
        QuizResponse quiz = publish(teacher, createQuiz(teacher, c, List.of(tf.id()), null, false).id());
        AttemptView victimAttempt = post(victim.login(),
                "/api/student/quizzes/" + quiz.id() + "/attempts", null, AttemptView.class);

        assertThat(putStatus(attacker.login(),
                "/api/student/quizzes/attempts/" + victimAttempt.attemptId(),
                new SubmitAttempt(List.of(answer(tf.id(), answerBoolean(false))), 0))).isEqualTo(404);
    }

    // ------------------------------------------------- manual grading + stats

    @Test
    @DisplayName("the teacher grades the essay and the attempt total updates")
    void manualGradingOfEssays() throws Exception {
        Actor admin = admin();
        Actor teacher = teacher();
        Course c = course(admin);
        var student = enrolledStudent(admin, "João Baptista", c.yearId(), c.turmaId());

        QuestionResponse tf = trueFalseQuestion(teacher, c, "Zero é par.", true);  // 1 point
        QuestionResponse essay = essayQuestion(teacher, c, "Explique.");           // 4 points
        QuizResponse quiz = publish(teacher, createQuiz(teacher, c,
                List.of(tf.id(), essay.id()), null, false).id());
        AttemptView view = post(student.login(), "/api/student/quizzes/" + quiz.id() + "/attempts",
                null, AttemptView.class);

        UUID essayAnswerId = UUID.randomUUID();
        post(student.login(), "/api/student/quizzes/attempts/" + view.attemptId() + "/submit",
                new SubmitAttempt(List.of(
                        answer(tf.id(), answerBoolean(true)),
                        new SubmitAnswer(essayAnswerId, essay.id(), answerText("Uma boa resposta."))), 0),
                AttemptResult.class);

        // The grade endpoint answers 200 with no body.
        assertThat(postStatus(teacher, "/api/quizzes/attempts/" + view.attemptId() + "/grade",
                new ManualGradeBatch(List.of(
                        new ManualGradeCommand(essayAnswerId, new BigDecimal("3.50"), "Bom raciocínio.")))))
                .isEqualTo(200);

        QuizAttemptRow row = get(teacher, "/api/quizzes/" + quiz.id() + "/attempts",
                new TypeReference<List<QuizAttemptRow>>() {}).getFirst();
        assertThat(row.autoScore()).isEqualByComparingTo("1.00");
        assertThat(row.manualScore()).isEqualByComparingTo("3.50");
        assertThat(row.status()).isEqualTo(AttemptStatus.GRADED);
    }

    @Test
    @DisplayName("re-grading an essay replaces the mark rather than adding to it")
    void reGradingReplacesTheMark() throws Exception {
        Actor admin = admin();
        Actor teacher = teacher();
        Course c = course(admin);
        var student = enrolledStudent(admin, "João Baptista", c.yearId(), c.turmaId());
        QuestionResponse essay = essayQuestion(teacher, c, "Explique.");
        QuizResponse quiz = publish(teacher, createQuiz(teacher, c, List.of(essay.id()), null, false).id());
        AttemptView view = post(student.login(), "/api/student/quizzes/" + quiz.id() + "/attempts",
                null, AttemptView.class);

        UUID answerId = UUID.randomUUID();
        post(student.login(), "/api/student/quizzes/attempts/" + view.attemptId() + "/submit",
                new SubmitAttempt(List.of(new SubmitAnswer(answerId, essay.id(), answerText("Resposta."))), 0),
                AttemptResult.class);

        postStatus(teacher, "/api/quizzes/attempts/" + view.attemptId() + "/grade",
                new ManualGradeBatch(List.of(new ManualGradeCommand(answerId, new BigDecimal("2.00"), "Razoável."))));
        postStatus(teacher, "/api/quizzes/attempts/" + view.attemptId() + "/grade",
                new ManualGradeBatch(List.of(new ManualGradeCommand(answerId, new BigDecimal("4.00"), "Reavaliado."))));

        QuizAttemptRow row = get(teacher, "/api/quizzes/" + quiz.id() + "/attempts",
                new TypeReference<List<QuizAttemptRow>>() {}).getFirst();
        assertThat(row.manualScore())
                .as("second grade should replace the first (4.00), not accumulate to 6.00")
                .isEqualByComparingTo("4.00");
    }

    @Test
    @DisplayName("analytics show per-question difficulty across submitted attempts only")
    void analyticsReportPerQuestionDifficulty() throws Exception {
        Actor admin = admin();
        Actor teacher = teacher();
        Course c = course(admin);
        var alunoA = enrolledStudent(admin, "Aluno A", c.yearId(), c.turmaId());
        var alunoB = enrolledStudent(admin, "Aluno B", c.yearId(), c.turmaId());
        var alunoC = enrolledStudent(admin, "Aluno C", c.yearId(), c.turmaId());

        QuestionResponse easy = trueFalseQuestion(teacher, c, "Fácil", true);
        QuestionResponse hard = mcQuestion(teacher, c, "Difícil", "d");
        QuizResponse quiz = publish(teacher, createQuiz(teacher, c,
                List.of(easy.id(), hard.id()), null, false).id());

        // A and B submit; both get the easy one right, only A gets the hard one.
        record Sat(EnrolledStudentFixture student, String hardChoice) {}
        for (Sat sat : List.of(new Sat(alunoA, "d"), new Sat(alunoB, "a"))) {
            Actor login = sat.student().login();
            AttemptView v = post(login, "/api/student/quizzes/" + quiz.id() + "/attempts",
                    null, AttemptView.class);
            post(login, "/api/student/quizzes/attempts/" + v.attemptId() + "/submit",
                    new SubmitAttempt(List.of(
                            answer(easy.id(), answerBoolean(true)),
                            answer(hard.id(), chooseOption(sat.hardChoice()))), 0), AttemptResult.class);
        }
        // C starts but never submits — an in-flight attempt must not skew the numbers.
        post(alunoC.login(), "/api/student/quizzes/" + quiz.id() + "/attempts", null, AttemptView.class);

        QuizAnalytics analytics = get(teacher, "/api/quizzes/" + quiz.id() + "/analytics", QuizAnalytics.class);

        assertThat(analytics.totalAttempts()).isEqualTo(3);
        assertThat(analytics.submittedAttempts()).isEqualTo(2);
        // A scored 1+2 = 3, B scored 1 → mean 2.00 out of a 3.00 total.
        assertThat(analytics.averageAutoScore()).isEqualByComparingTo("2.00");
        assertThat(analytics.averageTotalPoints()).isEqualByComparingTo("3.00");

        assertThat(analytics.questionStats())
                .filteredOn(s -> s.questionId().equals(easy.id())).singleElement()
                .satisfies(s -> {
                    assertThat(s.answeredCount()).isEqualTo(2);
                    assertThat(s.correctCount()).isEqualTo(2);
                    assertThat(s.correctRate()).isEqualByComparingTo("1.0000");
                });
        assertThat(analytics.questionStats())
                .filteredOn(s -> s.questionId().equals(hard.id())).singleElement()
                .extracting(QuestionStat::correctRate)
                .satisfies(rate -> assertThat((BigDecimal) rate).isEqualByComparingTo("0.5000"));
    }

    @Test
    @DisplayName("a student sees their own attempt history")
    void studentSeesOwnAttemptHistory() throws Exception {
        Actor admin = admin();
        Actor teacher = teacher();
        Course c = course(admin);
        var student = enrolledStudent(admin, "João Baptista", c.yearId(), c.turmaId());
        var other = enrolledStudent(admin, "Aluno B", c.yearId(), c.turmaId());

        QuestionResponse tf = trueFalseQuestion(teacher, c, "Zero é par.", true);
        QuizResponse quiz = publish(teacher, createQuiz(teacher, c, List.of(tf.id()), null, false).id());
        post(student.login(), "/api/student/quizzes/" + quiz.id() + "/attempts", null, AttemptView.class);
        post(other.login(), "/api/student/quizzes/" + quiz.id() + "/attempts", null, AttemptView.class);

        List<QuizAttemptRow> mine = get(student.login(), "/api/student/quizzes/attempts",
                new TypeReference<>() {});

        assertThat(mine).singleElement()
                .extracting(QuizAttemptRow::studentId).isEqualTo(student.studentId());
    }

    @Test
    @DisplayName("quiz authoring and analytics are teacher-only")
    void authoringIsTeacherOnly() throws Exception {
        Actor admin = admin();
        Actor teacher = teacher();
        Course c = course(admin);
        var student = enrolledStudent(admin, "João Baptista", c.yearId(), c.turmaId());
        QuestionResponse q = mcQuestion(teacher, c, "P1", "a");
        QuizResponse quiz = publish(teacher, createQuiz(teacher, c, List.of(q.id()), null, false).id());

        assertThat(postStatus(student.login(), "/api/questions",
                new CreateQuestion(c.subjectId(), "CLASSE_10", "Batota", QuestionType.TRUE_FALSE,
                        json.createObjectNode().put("correct", true), null))).isEqualTo(403);
        assertThat(getStatus(student.login(), "/api/quizzes/" + quiz.id() + "/analytics")).isEqualTo(403);
        assertThat(getStatus(student.login(), "/api/quizzes/" + quiz.id() + "/attempts")).isEqualTo(403);
        // The question bank itself must never be browsable by a student.
        assertThat(getStatus(student.login(), "/api/questions?subjectId=" + c.subjectId())).isEqualTo(403);
    }
}
