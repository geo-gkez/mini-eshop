package gr.unipi.eshop.catalog;

import gr.unipi.eshop.BaseIntegrationTest;
import gr.unipi.eshop.auth.AuthTestSupport;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class CatalogTest extends BaseIntegrationTest {

    // --- unauthenticated ---

    @Test
    void list_whenUnauthenticated_returns401() {
        given().spec(requestSpec)
                .when()
                .get("/api/products")
                .then()
                .statusCode(401);
    }

    // --- list (no query) ---

    @Test
    void list_whenAuthenticated_returnsAllProductsWithSearchInactive() {
        var session = AuthTestSupport.loginAsAlice(requestSpec);

        given().spec(requestSpec)
                .cookie(AuthTestSupport.SESSION_COOKIE, session)
                .when()
                .get("/api/products")
                .then()
                .statusCode(200)
                .body("searchActive", equalTo(false))
                .body("products", hasSize(greaterThan(0)))
                .body("pagination.page", equalTo(0))
                .body("pagination.totalPages", greaterThanOrEqualTo(1))
                .body("pagination.totalElements", greaterThan(0));
    }

    @Test
    void list_pageSizeAboveMax_returns400() {
        var session = AuthTestSupport.loginAsAlice(requestSpec);

        given().spec(requestSpec)
                .cookie(AuthTestSupport.SESSION_COOKIE, session)
                .queryParam("size", 1000)
                .when()
                .get("/api/products")
                .then()
                .statusCode(400);
    }

    // --- search ---

    @Test
    void search_whenQueryMatches_returnsFilteredProductsWithSearchActive() {
        var session = AuthTestSupport.loginAsAlice(requestSpec);

        given().spec(requestSpec)
                .cookie(AuthTestSupport.SESSION_COOKIE, session)
                .queryParam("search","Keyboard")
                .when()
                .get("/api/products")
                .then()
                .statusCode(200)
                .body("searchActive", equalTo(true))
                .body("products", hasSize(greaterThan(0)));
    }

    @Test
    void search_whenNoMatch_returnsEmptyWithSearchActive() {
        var session = AuthTestSupport.loginAsAlice(requestSpec);

        given().spec(requestSpec)
                .cookie(AuthTestSupport.SESSION_COOKIE, session)
                .queryParam("search","zzznotexisting")
                .when()
                .get("/api/products")
                .then()
                .statusCode(200)
                .body("searchActive", equalTo(true))
                .body("products", hasSize(0));
    }

    @Test
    void search_sqlInjectionReturnsEmpty() {
        var session = AuthTestSupport.loginAsAlice(requestSpec);

        given().spec(requestSpec)
                .cookie(AuthTestSupport.SESSION_COOKIE, session)
                .queryParam("search","' OR '1'='1")
                .when()
                .get("/api/products")
                .then()
                .statusCode(200)
                .body("searchActive", equalTo(true))
                .body("products", hasSize(0));
    }
}
