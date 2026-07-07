package ao.skool.common.web.error;

import org.springframework.http.HttpStatus;

public class NotFoundException extends ApplicationException {

    public NotFoundException() {
        super(HttpStatus.NOT_FOUND, "error.not_found");
    }

    public NotFoundException(String messageKey, Object... args) {
        super(HttpStatus.NOT_FOUND, messageKey, args);
    }
}
