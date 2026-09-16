package ao.skool.app.phase0;

import ao.skool.academic_structure.internal.domain.GradeLevel;
import ao.skool.academic_structure.internal.web.dto.SubjectDtos.CreateSubject;
import ao.skool.academic_structure.internal.web.dto.SubjectDtos.SubjectResponse;
import ao.skool.app.support.SchoolFixture;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 0 — the cross-cutting foundation: what is public, what needs a token, what an
 * error looks like on the wire, and whether one school can see another's data.
 */
class FoundationApiTest extends SchoolFixture {

    @Test
    @DisplayName("health probe is reachable without a token (load balancers have no JWT)")
    void healthIsPublic() throws Exception {
        assertThat(getStatus(anonymous(), "/actuator/health")).isEqualTo(200);
    }

    @Test
    @DisplayName("OpenAPI docs are not behind the auth wall")
    void openApiIsPublic() throws Exception {
        // Asserting "not blocked" rather than 200: springdoc cannot assemble the spec
        // under MockMvc (no real servlet container to enumerate server URLs from), so it
        // answers 500 here while serving fine on a running server. The security claim —
        // reachable without a token — is what this test is actually for.
        assertThat(getStatus(anonymous(), "/v3/api-docs")).isNotIn(401, 403);
    }

    @Test
    @DisplayName("business endpoints reject anonymous callers")
    void businessEndpointsRequireAuthentication() throws Exception {
        assertThat(getStatus(anonymous(), "/api/subjects")).isEqualTo(403);
    }

    @Test
    @DisplayName("a forged or corrupted token is treated as no token at all")
    void tamperedTokenIsRejected() throws Exception {
        Actor real = admin();
        // Flip the last character of the signature — same shape, wrong HMAC.
        String tampered = real.bearer().substring(0, real.bearer().length() - 1)
                + (real.bearer().endsWith("A") ? "B" : "A");
        Actor forged = new Actor(real.userId(), real.fullName(), real.email(), tampered);

        assertThat(getStatus(forged, "/api/subjects")).isEqualTo(403);
        assertThat(getStatus(new Actor(null, "x", null, "not-even-a-jwt"), "/api/subjects")).isEqualTo(403);
    }

    @Test
    @DisplayName("errors come back as RFC 7807 problem+json, localised to pt-AO")
    void errorsUseProblemDetail() throws Exception {
        var result = exec(admin(), MockMvcRequestBuilders.get("/api/students/" + java.util.UUID.randomUUID()), null);

        assertThat(result.getResponse().getStatus()).isEqualTo(404);
        assertThat(result.getResponse().getContentType()).startsWith("application/problem+json");

        JsonNode problem = json.readTree(bodyOf(result));
        assertThat(problem.get("type").asText()).isEqualTo("urn:skool:error:not_found");
        assertThat(problem.get("status").asInt()).isEqualTo(404);
        // pt-AO is the fixed default locale — an Angolan secretary should never see English.
        assertThat(problem.get("title").asText()).isEqualTo("Recurso não encontrado.");
        assertThat(problem.get("detail").asText()).isEqualTo("Recurso não encontrado.");
        assertThat(problem.get("instance").asText()).startsWith("/api/students/");
        assertThat(problem.hasNonNull("timestamp")).isTrue();
    }

    @Test
    @DisplayName("validation failures list the offending fields")
    void validationErrorsNameTheFields() throws Exception {
        // blank name, blank code, null gradeLevel
        var result = exec(admin(), MockMvcRequestBuilders.post("/api/subjects"),
                new CreateSubject("", "", null));

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        JsonNode problem = json.readTree(bodyOf(result));
        assertThat(problem.get("type").asText()).isEqualTo("urn:skool:error:validation");
        assertThat(problem.get("title").asText()).isEqualTo("Os dados fornecidos são inválidos.");

        List<String> fields = problem.get("errors").findValuesAsText("field");
        assertThat(fields).contains("name", "code", "gradeLevel");
    }

    @Test
    @DisplayName("a role that lacks permission gets 403 with the localised message")
    void roleGateReturnsForbidden() throws Exception {
        // Creating subjects is admin/director/secretary work — a teacher must not.
        var result = exec(teacher(), MockMvcRequestBuilders.post("/api/subjects"),
                new CreateSubject("Matemática", "MAT-X", GradeLevel.CLASSE_10));

        assertThat(result.getResponse().getStatus()).isEqualTo(403);
        JsonNode problem = json.readTree(bodyOf(result));
        assertThat(problem.get("type").asText()).isEqualTo("urn:skool:error:forbidden");
        assertThat(problem.get("title").asText()).isEqualTo("Não tem permissão para realizar esta ação.");
    }

    @Test
    @DisplayName("one school never sees another school's data")
    void tenantsAreIsolated() throws Exception {
        Actor ourAdmin = admin();
        SubjectResponse ours = createSubject(ourAdmin, "Física");

        Actor outsider = actorInOtherTenant("Admin Outra Escola", "ADMIN");
        List<SubjectResponse> theirList = get(outsider, "/api/subjects", new TypeReference<>() {});

        assertThat(theirList).extracting(SubjectResponse::id).doesNotContain(ours.id());
        assertThat(get(ourAdmin, "/api/subjects", new TypeReference<List<SubjectResponse>>() {}))
                .extracting(SubjectResponse::id).contains(ours.id());
    }
}
