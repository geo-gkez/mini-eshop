package gr.unipi.eshop.config;

import gr.unipi.eshop.shared.LogFields;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import static org.springframework.http.HttpStatus.*;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthenticationException() {
        var problemDetail = ProblemDetail.forStatus(UNAUTHORIZED);
        problemDetail.setTitle(UNAUTHORIZED.getReasonPhrase());

        return ResponseEntity.status(UNAUTHORIZED)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problemDetail);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation() {
        log.atWarn().addKeyValue(LogFields.Key.EVENT, LogFields.Event.VALIDATION_FAILURE).log("constraint violation");

        var problemDetail = ProblemDetail.forStatus(BAD_REQUEST);
        problemDetail.setTitle(BAD_REQUEST.getReasonPhrase());

        return ResponseEntity.status(BAD_REQUEST)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problemDetail);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrityViolation() {
        log.atWarn().addKeyValue(LogFields.Key.EVENT, LogFields.Event.VALIDATION_FAILURE).log("data integrity violation");

        var problemDetail = ProblemDetail.forStatus(CONFLICT);
        problemDetail.setTitle(CONFLICT.getReasonPhrase());
        problemDetail.setDetail("The request conflicts with the current state. Please retry.");

        return ResponseEntity.status(CONFLICT)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problemDetail);
    }

    // Last-resort catch-all for genuinely unexpected failures. The standard Spring MVC exceptions and
    // ResponseStatusException are handled more specifically by the superclass (so they keep their real
    // 4xx status); this only fires for the truly-unhandled. Log the stack trace for diagnosis, but
    // return a generic ProblemDetail — no message, class name, or stack trace — so nothing leaks.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
        log.atError().addKeyValue(LogFields.Key.EVENT, LogFields.Event.UNEXPECTED_ERROR).log("unhandled exception", ex);

        var problemDetail = ProblemDetail.forStatus(INTERNAL_SERVER_ERROR);
        problemDetail.setTitle(INTERNAL_SERVER_ERROR.getReasonPhrase());

        return ResponseEntity.status(INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problemDetail);
    }
}
