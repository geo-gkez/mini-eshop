package gr.unipi.eshop.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@NullMarked
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/auth", produces = MediaType.APPLICATION_JSON_VALUE)
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final LoginRateLimiter loginRateLimiter;
    private final SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserInfo> login(@Valid @RequestBody LoginRequest loginRequest,
                                          HttpServletRequest request,
                                          HttpServletResponse response) {
        // request.getRemoteAddr() returns the real client IP because Tomcat's RemoteIpValve
        // reads X-Forwarded-For set by nginx (internal-proxies matches 172.x.x.x Docker range)
        loginRateLimiter.checkLimit(loginRequest.username(), request.getRemoteAddr());

        try {
            var auth = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            loginRequest.username(), loginRequest.password()
                    )
            );

            loginRateLimiter.onSuccess(loginRequest.username());

            // Enforce maximumSessions(1): consult the registry, expire the oldest session,
            // rotate the session id (fixation defence), register the new one. formLogin/httpBasic
            // are disabled, so this is the only place the strategy gets invoked. Must run before saveContext.
            sessionAuthenticationStrategy.onAuthentication(auth, request, response);

            var context = securityContextHolderStrategy.createEmptyContext();
            context.setAuthentication(auth);
            securityContextHolderStrategy.setContext(context);
            securityContextRepository.saveContext(context, request, response);

            return ResponseEntity.ok(new UserInfo(auth.getName()));

        } catch (org.springframework.security.core.AuthenticationException e) {
            loginRateLimiter.onFailure(loginRequest.username(), request.getRemoteAddr());
            throw e;
        }
    }

    @GetMapping("/me")
    public ResponseEntity<UserInfo> me(@CurrentUser UserDetails userDetails,
                                       CsrfToken csrfToken) { // resolved by Spring MVC to force XSRF-TOKEN cookie write on every call
        if (userDetails == null) {
            throw new InsufficientAuthenticationException("Not authenticated");
        }

        return ResponseEntity.ok(new UserInfo(userDetails.getUsername()));
    }
}
