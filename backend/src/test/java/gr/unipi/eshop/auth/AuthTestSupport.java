package gr.unipi.eshop.auth;

import io.restassured.specification.RequestSpecification;

import java.util.Map;

import static io.restassured.RestAssured.given;

public class AuthTestSupport {

    /** Spring Session's default session cookie name (DefaultCookieSerializer); replaces the servlet container's JSESSIONID. */
    public static final String SESSION_COOKIE = "SESSION";

    public static String loginAsAlice(RequestSpecification requestSpec) {
        return login(requestSpec, "alice", "alicepass");
    }

    public static String loginAsBob(RequestSpecification requestSpec) {
        return login(requestSpec, "bob", "bobpass");
    }

    private static String login(RequestSpecification requestSpec, String username, String password) {
        return given().spec(requestSpec)
                .body(Map.of("username", username, "password", password))
                .when().post("/api/auth/login")
                .then().statusCode(200)
                .extract().cookie(SESSION_COOKIE);
    }
}
