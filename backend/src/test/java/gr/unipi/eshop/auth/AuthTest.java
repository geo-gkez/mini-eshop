package gr.unipi.eshop.auth;

import gr.unipi.eshop.BaseIntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

class AuthTest extends BaseIntegrationTest {
    // --- user-enumeration parity ---

    @Test
    void loginFailure_doesNotRevealWhetherUsernameExists() {
        int expectedStatus = 401;
        String expectedTitle = "Unauthorized";

        given().spec(requestSpec)
                .body(Map.of("username", "nobody", "password", "whatever"))
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(expectedStatus)
                .contentType("application/problem+json")
                .body("status", equalTo(expectedStatus))
                .body("title", equalTo(expectedTitle));

        given().spec(requestSpec)
                .body(Map.of("username", "alice", "password", "wrong"))
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(expectedStatus)
                .contentType("application/problem+json")
                .body("status", equalTo(expectedStatus))
                .body("title", equalTo(expectedTitle));
    }

    // --- successful login ---

    @Test
    void successfulLogin_returnsUsername() {
        given().spec(requestSpec)
                .body(Map.of("username", "alice", "password", "alicepass"))
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(200)
                .body("username", equalTo("alice"));
    }

    @Test
    void successfulLogin_rotatesSessionId() {
        // Establish a valid session by logging in once.
        String firstSession = given().spec(requestSpec)
                .body(Map.of("username", "alice", "password", "alicepass"))
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .cookie(AuthTestSupport.SESSION_COOKIE);

        // Re-login with that session: ID must be rotated (session-fixation defence).
        String rotatedSession = given().spec(requestSpec)
                .cookie(AuthTestSupport.SESSION_COOKIE, firstSession)
                .body(Map.of("username", "alice", "password", "alicepass"))
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(200)
                .extract().cookie(AuthTestSupport.SESSION_COOKIE);

        assertThat(rotatedSession).isNotNull().isNotEqualTo(firstSession);
    }

    // --- /me ---

    @Test
    void me_whenNotAuthenticated_returns401() {
        given().spec(requestSpec)
                .when().get("/api/auth/me")
                .then()
                .statusCode(401)
                .contentType("application/problem+json");
    }

    @Test
    void me_whenAuthenticated_returnsUsername() {
        String sessionCookie = given().spec(requestSpec)
                .body(Map.of("username", "alice", "password", "alicepass"))
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .cookie(AuthTestSupport.SESSION_COOKIE);

        given().spec(requestSpec)
                .cookie(AuthTestSupport.SESSION_COOKIE, sessionCookie)
                .when()
                .get("/api/auth/me")
                .then()
                .statusCode(200)
                .body("username", equalTo("alice"));
    }

    // --- logout ---

    @Test
    void logout_invalidatesSession() {
        String sessionCookie = given().spec(requestSpec)
                .body(Map.of("username", "alice", "password", "alicepass"))
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .cookie(AuthTestSupport.SESSION_COOKIE);

        given().spec(requestSpec)
                .cookie(AuthTestSupport.SESSION_COOKIE, sessionCookie)
                .when()
                .post("/api/auth/logout")
                .then()
                .statusCode(200);

        // Old session must no longer grant access to protected resources.
        given().spec(requestSpec)
                .cookie(AuthTestSupport.SESSION_COOKIE, sessionCookie)
                .when()
                .get("/api/products")
                .then()
                .statusCode(401);
    }
}
