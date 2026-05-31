package gr.unipi.eshop.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.session")
public record SessionProperties(

        // OWASP Session Management Cheat Sheet recommends 4–8h for office workers.
        // Spring Session issue #922 (open since 2017): absolute timeout is not natively
        // supported — enforced by AbsoluteSessionTimeoutInterceptor instead.
        @NotNull Duration absoluteTimeout

) {
}
