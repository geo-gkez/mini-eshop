package gr.unipi.eshop.config;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestControllerAdvice
public class GlobalExceptionHandler {

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
        var problemDetail = ProblemDetail.forStatus(BAD_REQUEST);
        problemDetail.setTitle(BAD_REQUEST.getReasonPhrase());

        return ResponseEntity.status(BAD_REQUEST)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problemDetail);
    }
}
