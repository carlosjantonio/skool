package ao.skool.common.web.error;

import org.springframework.http.HttpStatus;

/**
 * Base for any exception a module wants to surface as a specific HTTP problem.
 * The messageKey is resolved via i18n MessageSource in the global handler.
 */
public class ApplicationException extends RuntimeException {

    private final HttpStatus status;
    private final String messageKey;
    private final Object[] messageArgs;

    public ApplicationException(HttpStatus status, String messageKey, Object... messageArgs) {
        super(messageKey);
        this.status = status;
        this.messageKey = messageKey;
        this.messageArgs = messageArgs;
    }

    public HttpStatus status() {
        return status;
    }

    public String messageKey() {
        return messageKey;
    }

    public Object[] messageArgs() {
        return messageArgs;
    }
}
