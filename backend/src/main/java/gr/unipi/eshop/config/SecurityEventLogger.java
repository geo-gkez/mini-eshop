package gr.unipi.eshop.config;

import gr.unipi.eshop.shared.LogFields;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

@Slf4j
@NullMarked
@Component
public class SecurityEventLogger {

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        log.atInfo()
                .addKeyValue(LogFields.Key.EVENT, LogFields.Event.LOGIN_SUCCESS)
                .addKeyValue(LogFields.Key.USER, event.getAuthentication().getName())
                .log("login success user={}", event.getAuthentication().getName());
    }

    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent event) {
        log.atWarn()
                .addKeyValue(LogFields.Key.EVENT, LogFields.Event.LOGIN_FAILURE)
                .addKeyValue(LogFields.Key.USER, event.getAuthentication().getName())
                .log("login failure user={}", event.getAuthentication().getName());
    }
}
