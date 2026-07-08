package ao.skool.assessment.internal.service;

import ao.skool.assessment.internal.domain.Question;
import ao.skool.assessment.internal.persistence.QuestionRepository;
import ao.skool.assessment.internal.web.dto.AssessmentDtos.CreateQuestion;
import ao.skool.assessment.internal.web.dto.AssessmentDtos.QuestionResponse;
import ao.skool.common.domain.SkoolPrincipal;
import ao.skool.common.domain.tenant.TenantContext;
import ao.skool.common.web.error.NotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class QuestionService {

    private final QuestionRepository questions;
    private final TenantContext tenant;
    private final ObjectMapper mapper;

    public QuestionService(QuestionRepository questions, TenantContext tenant, ObjectMapper mapper) {
        this.questions = questions;
        this.tenant = tenant;
        this.mapper = mapper;
    }

    public QuestionResponse create(CreateQuestion cmd) {
        var tenantId = tenant.current().value();
        BigDecimal points = cmd.points() != null ? cmd.points() : BigDecimal.ONE;
        String payloadJson = writeJson(cmd.payload());
        Question q = new Question(UUID.randomUUID(), tenantId, cmd.subjectId(),
                cmd.gradeLevel(), cmd.prompt(), cmd.questionType(),
                payloadJson, points, currentActor());
        questions.save(q);
        return toResponse(q);
    }

    @Transactional(readOnly = true)
    public List<QuestionResponse> listBySubject(UUID subjectId, String gradeLevel) {
        var tenantId = tenant.current().value();
        var list = gradeLevel == null || gradeLevel.isBlank()
                ? questions.findByTenantIdAndSubjectIdOrderByCreatedAtDesc(tenantId, subjectId)
                : questions.findByTenantIdAndSubjectIdAndGradeLevelOrderByCreatedAtDesc(tenantId, subjectId, gradeLevel);
        return list.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public QuestionResponse get(UUID id) {
        Question q = questions.findById(id).orElseThrow(NotFoundException::new);
        return toResponse(q);
    }

    private QuestionResponse toResponse(Question q) {
        return new QuestionResponse(q.id(), q.subjectId(), q.gradeLevel(), q.prompt(),
                q.questionType(), readJson(q.payload()), q.points(), q.createdAt());
    }

    private String writeJson(JsonNode node) {
        try {
            return mapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("invalid payload", e);
        }
    }

    private JsonNode readJson(String raw) {
        try {
            return mapper.readTree(raw);
        } catch (JsonProcessingException e) {
            return mapper.createObjectNode();
        }
    }

    private UUID currentActor() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof SkoolPrincipal p) {
            try { return UUID.fromString(p.userId()); } catch (IllegalArgumentException ignored) { return null; }
        }
        return null;
    }
}
