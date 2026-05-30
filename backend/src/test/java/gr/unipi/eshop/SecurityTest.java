package gr.unipi.eshop;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

class SecurityTest extends BaseIntegrationTest {

    @Test
    void unauthenticated_getProducts_returns401ProblemDetail() {
        given()
                .spec(requestSpec)
        .when()
                .get("/api/products")
        .then()
                .statusCode(401)
                .contentType("application/problem+json")
                .body("title", equalTo("Unauthorized"))
                .body("status", equalTo(401));
    }

    // --- CSRF ---

    @Test
    void post_withoutCsrfToken_returns403() {
        given()
                .port(port)
                .contentType(ContentType.JSON)
                .body(Map.of("username", "alice", "password", "alicepass"))
        .when()
                .post("/api/auth/login")
        .then()
                .statusCode(403);
    }

    @Test
    void post_withMismatchedCsrfToken_returns403() {
        given()
                .port(port)
                .contentType(ContentType.JSON)
                .cookie("XSRF-TOKEN", "token-in-cookie")
                .header("X-XSRF-TOKEN", "different-token-in-header")
                .body(Map.of("username", "alice", "password", "alicepass"))
        .when()
                .post("/api/auth/login")
        .then()
                .statusCode(403);
    }

    @Test
    void getMe_setsXsrfTokenCookie() {
        given()
                .port(port)
        .when()
                .get("/api/auth/me")
        .then()
                .cookie("XSRF-TOKEN");
    }
}
