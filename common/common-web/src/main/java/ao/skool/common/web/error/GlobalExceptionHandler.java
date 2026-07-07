package ao.skool.common.web.error;

import org.springframework.context.MessageSource;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * RFC 7807 problem+json responses, localized via MessageSource.
 * Every response includes:
 *   - type: a stable URN so clients can pattern-match without parsing strings
 *   - title: localized short message
 *   - status: HTTP status
 *   - detail: localized long message (defaults to title when no detail key is set)
 *   - instance: request path
 *   - timestamp: ISO-8601
 *   - traceId: correlation ID when present in MDC
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ProblemDetail> handleApplication(ApplicationException ex, Locale locale) {
        return problem(ex.status().value(), ex.messageKey(), ex.messageArgs(), locale);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex, Locale locale) {
        var body = problem(400, "error.validation", new Object[0], locale);
        List<Map<String, String>> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> Map.of(
                        "field", fe.getField(),
                        "message", fe.getDefaultMessage() == null ? "" : fe.getDefaultMessage()))
                .toList();
        body.getBody().setProperty("errors", fieldErrors);
        return body;
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuth(AuthenticationException ex, Locale locale) {
        return problem(401, "error.unauthorized", new Object[0], locale);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleForbidden(AccessDeniedException ex, Locale locale) {
        return problem(403, "error.forbidden", new Object[0], locale);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleAny(Exception ex, Locale locale) {
        return problem(500, "error.internal", new Object[0], locale);
    }

    private ResponseEntity<ProblemDetail> problem(int status, String key, Object[] args, Locale locale) {
        String title = messageSource.getMessage(key, args, key, locale);
        ProblemDetail body = ProblemDetail.forStatus(status);
        body.setTitle(title);
        body.setDetail(title);
        body.setType(URI.create("urn:skool:" + key.replace('.', ':')));

        HttpServletRequest req = currentRequest();
        if (req != null) {
            body.setInstance(URI.create(req.getRequestURI()));
        }
        Map<String, Object> extras = new LinkedHashMap<>();
        extras.put("timestamp", Instant.now().toString());
        String traceId = org.slf4j.MDC.get("traceId");
        if (traceId != null) {
            extras.put("traceId", traceId);
        }
        extras.forEach(body::setProperty);
        return ResponseEntity.status(status).body(body);
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes sra) {
            return sra.getRequest();
        }
        return null;
    }
}
