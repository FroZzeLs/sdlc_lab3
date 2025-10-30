package by.frozzel.springreviewer.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class LogAccessException extends RuntimeException {
    public LogAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}