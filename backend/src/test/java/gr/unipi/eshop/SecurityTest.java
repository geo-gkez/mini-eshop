package gr.unipi.eshop;

import org.junit.jupiter.api.Test;

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
}
