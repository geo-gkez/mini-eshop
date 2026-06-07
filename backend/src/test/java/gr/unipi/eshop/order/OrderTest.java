package gr.unipi.eshop.order;

import ch.martinelli.oss.testcontainers.mailpit.MailpitClient;
import gr.unipi.eshop.BaseIntegrationTest;
import gr.unipi.eshop.auth.AuthTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class OrderTest extends BaseIntegrationTest {

    @Autowired
    MailpitClient mailpitClient;

    @BeforeEach
    void clearEmails() {
        mailpitClient.deleteAllMessages();
    }

    private UUID anyProductReference(String session) {
        return UUID.fromString(
                given().spec(requestSpec)
                        .cookie(AuthTestSupport.SESSION_COOKIE, session)
                        .when().get("/api/products")
                        .then().statusCode(200)
                        .extract().
                        jsonPath()
                        .getString("products[0].reference")
        );
    }

    private void addItemToCart(String session, UUID ref) {
        given().spec(requestSpec)
                .cookie(AuthTestSupport.SESSION_COOKIE, session)
                .body(Map.of("productReference", ref.toString(), "quantity", 2))
                .when()
                .post("/api/cart/items")
                .then()
                .statusCode(200);
    }

    private Map<String, String> validAddress() {
        return Map.of(
                "street", "123 Main St",
                "city", "Athens",
                "postalCode", "10432",
                "country", "Greece"
        );
    }

    // --- unauthenticated ---

    @Test
    void submit_whenUnauthenticated_returns401() {
        given().spec(requestSpec)
                .body(validAddress())
                .when()
                .post("/api/order/submit")
                .then()
                .statusCode(401);
    }

    @Test
    void confirm_whenUnauthenticated_returns401() {
        given().spec(requestSpec)
                .when()
                .post("/api/order/confirm")
                .then()
                .statusCode(401);
    }

    // --- validation ---

    @Test
    void submit_whenCartEmpty_returns400() {
        var session = AuthTestSupport.loginAsAlice(requestSpec);

        given().spec(requestSpec)
                .cookie(AuthTestSupport.SESSION_COOKIE, session)
                .body(validAddress())
                .when()
                .post("/api/order/submit")
                .then()
                .statusCode(400);
    }

    @Test
    void submit_withCrlfInStreet_returns400() {
        var session = AuthTestSupport.loginAsAlice(requestSpec);
        var ref = anyProductReference(session);
        addItemToCart(session, ref);

        given().spec(requestSpec)
                .cookie(AuthTestSupport.SESSION_COOKIE, session)
                .body(Map.of(
                        "street", "123 Main St\r\nBcc: evil@example.com",
                        "city", "Athens",
                        "postalCode", "10432",
                        "country", "Greece"
                ))
                .when()
                .post("/api/order/submit")
                .then()
                .statusCode(400);
    }

    @Test
    void submit_withNewlineInCity_returns400() {
        var session = AuthTestSupport.loginAsAlice(requestSpec);
        var ref = anyProductReference(session);
        addItemToCart(session, ref);

        given().spec(requestSpec)
                .cookie(AuthTestSupport.SESSION_COOKIE, session)
                .body(Map.of(
                        "street", "123 Main St",
                        "city", "Athens\nevil",
                        "postalCode", "10432",
                        "country", "Greece"
                ))
                .when()
                .post("/api/order/submit")
                .then()
                .statusCode(400);
    }

    @Test
    void confirm_withoutPriorSubmit_returns400() {
        var session = AuthTestSupport.loginAsAlice(requestSpec);

        given().spec(requestSpec)
                .cookie(AuthTestSupport.SESSION_COOKIE, session)
                .when()
                .post("/api/order/confirm")
                .then().statusCode(400);
    }

    // --- happy path ---

    @Test
    void submit_returnsOrderReviewWithCartItems() {
        var session = AuthTestSupport.loginAsAlice(requestSpec);
        var ref = anyProductReference(session);
        addItemToCart(session, ref);

        given().spec(requestSpec)
                .cookie(AuthTestSupport.SESSION_COOKIE, session)
                .body(validAddress())
                .when()
                .post("/api/order/submit")
                .then()
                .statusCode(200)
                .body("address.city", equalTo("Athens"))
                .body("address.country", equalTo("Greece"))
                .body("lines", hasSize(1))
                .body("lines[0].quantity", equalTo(2))
                .body("total", notNullValue());
    }

    @Test
    void fullOrderFlow_sendsEmailAndClearsCart() {
        var session = AuthTestSupport.loginAsAlice(requestSpec);
        var ref = anyProductReference(session);
        addItemToCart(session, ref);

        given().spec(requestSpec)
                .cookie(AuthTestSupport.SESSION_COOKIE, session)
                .body(validAddress())
                .when()
                .post("/api/order/submit")
                .then()
                .statusCode(200);

        given().spec(requestSpec)
                .cookie(AuthTestSupport.SESSION_COOKIE, session)
                .when()
                .post("/api/order/confirm")
                .then()
                .statusCode(204);

        // verify email was sent to admin
        assertThat(mailpitClient.getMessageCount(), equalTo(1));
        var msg = mailpitClient.getAllMessages().getFirst();
        assertThat(msg.subject(), containsString("Alice"));
        assertThat(msg.to().getFirst().address(), equalTo("admin@test.local"));

        // cart must be cleared after confirm
        given().spec(requestSpec)
                .cookie(AuthTestSupport.SESSION_COOKIE, session)
                .when()
                .get("/api/cart")
                .then()
                .statusCode(200)
                .body("items", hasSize(0));
    }

    @Test
    void confirm_clearsOrderFromSession_cannotConfirmTwice() {
        var session = AuthTestSupport.loginAsAlice(requestSpec);
        var ref = anyProductReference(session);
        addItemToCart(session, ref);

        given().spec(requestSpec)
                .cookie(AuthTestSupport.SESSION_COOKIE, session)
                .body(validAddress())
                .when()
                .post("/api/order/submit")
                .then()
                .statusCode(200);

        given().spec(requestSpec)
                .cookie(AuthTestSupport.SESSION_COOKIE, session)
                .when()
                .post("/api/order/confirm")
                .then()
                .statusCode(204);

        // second confirm should fail — no pending order in session
        given().spec(requestSpec)
                .cookie(AuthTestSupport.SESSION_COOKIE, session)
                .when()
                .post("/api/order/confirm")
                .then()
                .statusCode(400);
    }
}
