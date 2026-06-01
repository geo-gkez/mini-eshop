package gr.unipi.eshop.auth;

import gr.unipi.eshop.BaseIntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * App-layer (per-username) login throttle — {@link LoginRateLimiter}.
 * <p>
 * These tests hit the backend directly (RANDOM_PORT), bypassing nginx, so they exercise the
 * credential-aware throttle in isolation. The nginx {@code limit_req} edge limit (volumetric,
 * per-IP) is a separate layer demonstrated against the running Docker stack, not here.
 * <p>
 * Counters are cleared before each test by {@code BaseIntegrationTest.resetLoginRateLimitCounters()}.
 */
class LoginRateLimitTest extends BaseIntegrationTest {

    private static final int USERNAME_THRESHOLD = 10;

    private int attemptLogin(String username, String password) {
        return given().spec(requestSpec)
                .body(Map.of("username", username, "password", password))
                .when()
                .post("/api/auth/login")
                .then()
                .extract()
                .statusCode();
    }

    @Test
    void login_afterTenFailures_isThrottledWith429() {
        for (int i = 0; i < USERNAME_THRESHOLD; i++) {
            // The first THRESHOLD attempts are merely rejected as bad credentials (401).
            assertThat(attemptLogin("alice", "wrongpass")).isEqualTo(401);
        }

        // The next attempt is throttled before authentication is even tried.
        assertThat(attemptLogin("alice", "wrongpass")).isEqualTo(429);
    }

    @Test
    void login_whenThrottled_blocksEvenCorrectPassword() {
        for (int i = 0; i < USERNAME_THRESHOLD; i++) {
            attemptLogin("alice", "wrongpass");
        }

        // Correct credentials are still rejected with 429 while the lockout window is open:
        // the throttle check runs before authentication, so it cannot be bypassed by guessing right.
        given().spec(requestSpec)
                .body(Map.of("username", "alice", "password", "alicepass"))
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(429);
    }

    @Test
    void login_throttleIsPerUsername_otherUserUnaffected() {
        for (int i = 0; i < USERNAME_THRESHOLD; i++) {
            attemptLogin("alice", "wrongpass");
        }

        // bob shares the client IP but has his own username counter, and the IP threshold (20)
        // is higher than alice's 10 failures — so bob still logs in successfully.
        given().spec(requestSpec)
                .body(Map.of("username", "bob", "password", "bobpass"))
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(200)
                .body("username", equalTo("bob"));
    }
}
