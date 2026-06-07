package gr.unipi.eshop.order;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.order")
public record OrderProperties(
        @NotBlank @Email
        String adminEmail,
        @NotBlank @Email @DefaultValue("no-reply@example.com")
        String fromEmail
) {
}
