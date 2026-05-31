package gr.unipi.eshop.cart;

import gr.unipi.eshop.BaseIntegrationTest;
import gr.unipi.eshop.auth.AuthTestSupport;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class CartTest extends BaseIntegrationTest {

    private UUID anyProductReference(String session) {
        return UUID.fromString(
                given().spec(requestSpec)
                        .cookie(AuthTestSupport.SESSION_COOKIE, session)
                        .when().get("/api/products")
                        .then().statusCode(200)
                        .extract().jsonPath().getString("products[0].reference")
        );
    }

    // --- unauthenticated ---

    @Test
    void view_whenUnauthenticated_returns401() {
        given().spec(requestSpec)
                .when()
                .get("/api/cart")
                .then()
                .statusCode(401);
    }

    // --- empty cart ---

    @Test
    void view_emptyCart_returnsEmptyItems() {
        var session = AuthTestSupport.loginAsAlice(requestSpec);

        given().spec(requestSpec)
                .cookie(AuthTestSupport.SESSION_COOKIE, session)
                .when()
                .get("/api/cart")
                .then()
                .statusCode(200)
                .body("items", hasSize(0))
                .body("total", equalTo(0));
    }

    // --- add ---

    @Test
    void addItem_validProduct_returnsCartWithItem() {
        var session = AuthTestSupport.loginAsAlice(requestSpec);
        var ref = anyProductReference(session);

        given().spec(requestSpec)
                .cookie(AuthTestSupport.SESSION_COOKIE, session)
                .body(Map.of("productReference", ref.toString(), "quantity", 2))
                .when()
                .post("/api/cart/items")
                .then()
                .statusCode(201)
                .body("items", hasSize(1))
                .body("items[0].quantity", equalTo(2));
    }

    @Test
    void addItem_unknownProduct_returns404() {
        var session = AuthTestSupport.loginAsAlice(requestSpec);

        given().spec(requestSpec)
                .cookie(AuthTestSupport.SESSION_COOKIE, session)
                .body(Map.of("productReference", UUID.randomUUID().toString(), "quantity", 1))
                .when()
                .post("/api/cart/items")
                .then()
                .statusCode(404);
    }

    @Test
    void addItem_zeroQuantity_returns400() {
        var session = AuthTestSupport.loginAsAlice(requestSpec);
        var ref = anyProductReference(session);

        given().spec(requestSpec)
                .cookie(AuthTestSupport.SESSION_COOKIE, session)
                .body(Map.of("productReference", ref.toString(), "quantity", 0))
                .when()
                .post("/api/cart/items")
                .then().statusCode(400);
    }

    @Test
    void addItem_sameProductTwice_aggregatesQuantity() {
        var session = AuthTestSupport.loginAsAlice(requestSpec);
        var ref = anyProductReference(session);

        given().spec(requestSpec)
                .cookie(AuthTestSupport.SESSION_COOKIE, session)
                .body(Map.of("productReference", ref.toString(), "quantity", 1))
                .when()
                .post("/api/cart/items")
                .then().
                statusCode(201);

        given().spec(requestSpec)
                .cookie(AuthTestSupport.SESSION_COOKIE, session)
                .body(Map.of("productReference", ref.toString(), "quantity", 2))
                .when()
                .post("/api/cart/items")
                .then()
                .statusCode(201)
                .body("items", hasSize(1))
                .body("items[0].quantity", equalTo(3));
    }

    // --- update ---

    @Test
    void updateItem_changesQuantity() {
        var session = AuthTestSupport.loginAsAlice(requestSpec);
        var ref = anyProductReference(session);

        given().spec(requestSpec)
                .cookie(AuthTestSupport.SESSION_COOKIE, session)
                .body(Map.of("productReference", ref.toString(), "quantity", 1))
                .when()
                .post("/api/cart/items").then().statusCode(201);

        given().spec(requestSpec)
                .cookie(AuthTestSupport.SESSION_COOKIE, session)
                .body(Map.of("quantity", 5))
                .when()
                .patch("/api/cart/items/" + ref)
                .then()
                .statusCode(200)
                .body("items[0].quantity", equalTo(5));
    }

    @Test
    void updateItem_notInCart_returns404() {
        var session = AuthTestSupport.loginAsAlice(requestSpec);

        given().spec(requestSpec)
                .cookie(AuthTestSupport.SESSION_COOKIE, session)
                .body(Map.of("quantity", 1))
                .when()
                .patch("/api/cart/items/" + UUID.randomUUID())
                .then()
                .statusCode(404);
    }

    // --- remove ---

    @Test
    void removeItem_removesFromCart() {
        var session = AuthTestSupport.loginAsAlice(requestSpec);
        var ref = anyProductReference(session);

        given().spec(requestSpec)
                .cookie(AuthTestSupport.SESSION_COOKIE, session)
                .body(Map.of("productReference", ref.toString(), "quantity", 1))
                .when()
                .post("/api/cart/items").then().statusCode(201);

        given().spec(requestSpec)
                .cookie(AuthTestSupport.SESSION_COOKIE, session)
                .when()
                .delete("/api/cart/items/" + ref)
                .then().statusCode(204);

        given().spec(requestSpec)
                .cookie(AuthTestSupport.SESSION_COOKIE, session)
                .when()
                .get("/api/cart")
                .then()
                .statusCode(200)
                .body("items", hasSize(0));
    }

    @Test
    void removeItem_notInCart_returns404() {
        var session = AuthTestSupport.loginAsAlice(requestSpec);

        given().spec(requestSpec)
                .cookie(AuthTestSupport.SESSION_COOKIE, session)
                .when()
                .delete("/api/cart/items/" + UUID.randomUUID())
                .then()
                .statusCode(404);
    }

    // --- user isolation ---

    @Test
    void cart_isUserScoped_aliceAndBobHaveSeparateCarts() {
        var aliceSession = AuthTestSupport.loginAsAlice(requestSpec);
        var bobSession = AuthTestSupport.loginAsBob(requestSpec);
        var ref = anyProductReference(aliceSession);

        given().spec(requestSpec)
                .cookie(AuthTestSupport.SESSION_COOKIE, aliceSession)
                .body(Map.of("productReference", ref.toString(), "quantity", 1))
                .when()
                .post("/api/cart/items").then().statusCode(201);

        given().spec(requestSpec)
                .cookie(AuthTestSupport.SESSION_COOKIE, bobSession)
                .when()
                .get("/api/cart")
                .then()
                .statusCode(200)
                .body("items", hasSize(0));
    }

    // --- persistence across sessions (the point of the DB-backed cart) ---

    @Test
    void cart_persistsAcrossLogoutAndLogin() {
        var firstSession = AuthTestSupport.loginAsAlice(requestSpec);
        var ref = anyProductReference(firstSession);

        given().spec(requestSpec)
                .cookie(AuthTestSupport.SESSION_COOKIE, firstSession)
                .body(Map.of("productReference", ref.toString(), "quantity", 3))
                .when()
                .post("/api/cart/items").then().statusCode(201);

        // Log out, then log in again — a brand-new session.
        given().spec(requestSpec)
                .cookie(AuthTestSupport.SESSION_COOKIE, firstSession)
                .when().post("/api/auth/logout").then().statusCode(200);

        var secondSession = AuthTestSupport.loginAsAlice(requestSpec);

        given().spec(requestSpec)
                .cookie(AuthTestSupport.SESSION_COOKIE, secondSession)
                .when()
                .get("/api/cart")
                .then()
                .statusCode(200)
                .body("items", hasSize(1))
                .body("items[0].quantity", equalTo(3));
    }
}
