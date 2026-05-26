package gr.unipi.eshop.catalog;

import gr.unipi.eshop.BaseIntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class CatalogTest extends BaseIntegrationTest {

    private String loginAsAlice() {
        return given().spec(requestSpec)
                .body(Map.of("username", "alice", "password", "alicepass"))
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .cookie("JSESSIONID");
    }

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
        var session = loginAsAlice();

        given().spec(requestSpec)
                .cookie("JSESSIONID", session)
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
        var session = loginAsAlice();

        given().spec(requestSpec)
                .cookie("JSESSIONID", session)
                .queryParam("size", 1000)
                .when()
                .get("/api/products")
                .then()
                .statusCode(400);
    }

    // --- search ---

    @Test
    void search_whenQueryMatches_returnsFilteredProductsWithSearchActive() {
        var session = loginAsAlice();

        given().spec(requestSpec)
                .cookie("JSESSIONID", session)
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
        var session = loginAsAlice();

        given().spec(requestSpec)
                .cookie("JSESSIONID", session)
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
        var session = loginAsAlice();

        given().spec(requestSpec)
                .cookie("JSESSIONID", session)
                .queryParam("search","' OR '1'='1")
                .when()
                .get("/api/products")
                .then()
                .statusCode(200)
                .body("searchActive", equalTo(true))
                .body("products", hasSize(0));
    }
}
