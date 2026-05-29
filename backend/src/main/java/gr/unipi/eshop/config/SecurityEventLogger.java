package gr.unipi.eshop.config;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

@NullMarked
@Component
public class SecurityEventLogger {

    private static final Logger log = LoggerFactory.getLogger(SecurityEventLogger.class);

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        log.atInfo()
                .addKeyValue("event", "LOGIN_SUCCESS")
                .addKeyValue("user", event.getAuthentication().getName())
                .log("login success");
    }

    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent event) {
        log.atWarn()
                .addKeyValue("event", "LOGIN_FAILURE")
                .addKeyValue("user", event.getAuthentication().getName())
                .log("login failure");
    }
}
