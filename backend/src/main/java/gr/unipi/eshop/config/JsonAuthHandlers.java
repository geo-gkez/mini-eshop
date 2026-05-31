package gr.unipi.eshop.config;

import gr.unipi.eshop.shared.LogFields;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@NullMarked
@RequiredArgsConstructor
public class JsonAuthHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ProblemDetailWriter writer;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        log.atWarn()
                .addKeyValue(LogFields.Key.EVENT, LogFields.Event.UNAUTHENTICATED_ACCESS)
                .addKeyValue(LogFields.Key.PATH, request.getRequestURI())
                .log("unauthenticated access path={}", request.getRequestURI());
        writer.write(response, HttpStatus.UNAUTHORIZED);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        log.atWarn()
                .addKeyValue(LogFields.Key.EVENT, LogFields.Event.ACCESS_DENIED)
                .addKeyValue(LogFields.Key.PATH, request.getRequestURI())
                .log("access denied path={}", request.getRequestURI());
        writer.write(response, HttpStatus.FORBIDDEN);
    }
}
