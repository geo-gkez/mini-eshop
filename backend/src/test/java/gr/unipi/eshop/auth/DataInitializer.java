package gr.unipi.eshop.auth;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;

@TestComponent
public class DataInitializer {

    private final AppUserRepository users;
    private final PasswordEncoder encoder;

    DataInitializer(AppUserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @EventListener(ApplicationReadyEvent.class)
    void seed() {
        if (users.count() > 0) return;
        users.save(AppUser.builder().username("alice").passwordHash(encoder.encode("alicepass")).firstName("Alice").lastName("Smith").email("alice@example.com").build());
        users.save(AppUser.builder().username("bob")  .passwordHash(encoder.encode("bobpass"))  .firstName("Bob")  .lastName("Jones").email("bob@example.com")  .build());
    }
}
