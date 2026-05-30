package gr.unipi.eshop.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
    private final SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserInfo> login(@RequestBody LoginRequest loginRequest,
                                          HttpServletRequest request,
                                          HttpServletResponse response) {
        var auth = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        loginRequest.username(), loginRequest.password()
                )
        );

        var context = securityContextHolderStrategy.createEmptyContext();
        context.setAuthentication(auth);

        securityContextHolderStrategy.setContext(context);

        // Save first (creates/updates session), then rotate ID for session-fixation defence.
        // changeSessionId() preserves session data while issuing a new session token.
        securityContextRepository.saveContext(context, request, response);

        request.changeSessionId();

        return ResponseEntity.ok(new UserInfo(auth.getName()));
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
