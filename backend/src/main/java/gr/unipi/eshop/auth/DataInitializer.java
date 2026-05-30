package gr.unipi.eshop.auth;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
class DataInitializer {

    private final AppUserRepository users;
    private final PasswordEncoder encoder;

    DataInitializer(AppUserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @EventListener(ApplicationReadyEvent.class)
    void seed() {
        if (users.count() > 0) return;
        users.save(new AppUser("alice", encoder.encode("alicepass"), "Alice", "Smith", "alice@example.com"));
        users.save(new AppUser("bob",   encoder.encode("bobpass"),   "Bob",   "Jones", "bob@example.com"));
    }
}
