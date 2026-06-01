package gr.unipi.eshop.auth;

import gr.unipi.eshop.BaseIntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Verifies maximumSessions(1) concurrency control. formLogin/httpBasic are disabled, so the
 * limit is only enforced because AuthController.login invokes the SessionAuthenticationStrategy
 * explicitly. These tests lock that behaviour in so the enforcement can't silently regress.
 */
class ConcurrentSessionTest extends BaseIntegrationTest {

    /** Logs in as the given user on a fresh session (no SESSION cookie sent) and returns the new session cookie. */
    private String loginFreshSession(String username, String password) {
        return given().spec(requestSpec)
                .body(Map.of("username", username, "password", password))
                .when().post("/api/auth/login")
                .then().statusCode(200)
                .extract().cookie(AuthTestSupport.SESSION_COOKIE);
    }

    @Test
    void secondLoginForSameUser_expiresTheFirstSession() {
        // Browser 1 ("Firefox") logs in and has a working session.
        String firstSession = loginFreshSession("alice", "alicepass");
        given().spec(requestSpec)
                .cookie(AuthTestSupport.SESSION_COOKIE, firstSession)
                .when().get("/api/auth/me")
                .then().statusCode(200).body("username", equalTo("alice"));

        // Browser 2 ("Brave") logs in as the same user on a separate session.
        String secondSession = loginFreshSession("alice", "alicepass");
        assertThat(secondSession).isNotNull().isNotEqualTo(firstSession);

        // The newest session is the live one.
        given().spec(requestSpec)
                .cookie(AuthTestSupport.SESSION_COOKIE, secondSession)
                .when().get("/api/auth/me")
                .then().statusCode(200).body("username", equalTo("alice"));

        // The first session must now be expired by the concurrency control, surfacing our 401 JSON.
        given().spec(requestSpec)
                .cookie(AuthTestSupport.SESSION_COOKIE, firstSession)
                .when().get("/api/products")
                .then()
                .statusCode(401)
                .contentType("application/problem+json")
                .body("detail", equalTo("You have been logged in from another device."));
    }

    @Test
    void limitIsPerUser_loginAsAnotherUserDoesNotExpireAlice() {
        String aliceSession = loginFreshSession("alice", "alicepass");
        String bobSession = loginFreshSession("bob", "bobpass");

        // Different principals -> the per-user limit is independent; both sessions stay live.
        given().spec(requestSpec)
                .cookie(AuthTestSupport.SESSION_COOKIE, aliceSession)
                .when().get("/api/auth/me")
                .then().statusCode(200).body("username", equalTo("alice"));

        given().spec(requestSpec)
                .cookie(AuthTestSupport.SESSION_COOKIE, bobSession)
                .when().get("/api/auth/me")
                .then().statusCode(200).body("username", equalTo("bob"));
    }
}
