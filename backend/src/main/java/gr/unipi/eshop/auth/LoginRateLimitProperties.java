package gr.unipi.eshop.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Tunable policy for {@link LoginRateLimiter}. Defaults guard against a missing property
 * silently disabling the limit (a zero threshold would mean "block immediately"/"never block").
 */
@ConfigurationProperties(prefix = "app.login-rate-limit")
public record LoginRateLimitProperties(
        @DefaultValue("10") int usernameThreshold,
        @DefaultValue("20") int ipThreshold,
        @DefaultValue("15m") Duration window) {
}
