package gr.unipi.eshop.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.time.Instant;

@NullMarked
@Component
@RequiredArgsConstructor
public class AbsoluteSessionTimeoutInterceptor implements HandlerInterceptor {

    private final SessionProperties sessionProperties;
    private final ProblemDetailWriter writer;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        var session = request.getSession(false);

        if (session == null)
            return true;

        var age = Duration.between(
                Instant.ofEpochMilli(session.getCreationTime()),
                Instant.now()
        );

        if (age.compareTo(sessionProperties.absoluteTimeout()) > 0) {
            session.invalidate();
            writer.write(response, HttpStatus.UNAUTHORIZED, "Maximum session duration exceeded. Please log in again.");

            return false;
        }

        return true;
    }
}
