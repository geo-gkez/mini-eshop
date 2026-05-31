package gr.unipi.eshop;

import gr.unipi.eshop.auth.DataInitializer;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;


@Import({TestcontainersConfiguration.class, DataInitializer.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseIntegrationTest {

    @LocalServerPort
    protected int port;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    protected RequestSpecification requestSpec;

    @BeforeEach
    void resetCartAndOrderState() {
        // The cart and pending orders persist per-user in the DB now, so isolate tests by clearing
        // them between runs. Users and products are seeded and must remain.
        jdbcTemplate.execute("TRUNCATE cart_items, carts, order_lines, orders RESTART IDENTITY CASCADE");
    }

    @BeforeEach
    void setUpBase() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        // CookieCsrfTokenRepository validates that the X-XSRF-TOKEN header matches the XSRF-TOKEN cookie.
        // Injecting both with the same value bypasses the need for a server roundtrip to bootstrap the cookie.
        String csrfToken = UUID.randomUUID().toString();
        requestSpec = new RequestSpecBuilder()
                .setPort(port)
                .setContentType(ContentType.JSON)
                .addCookie("XSRF-TOKEN", csrfToken)
                .addHeader("X-XSRF-TOKEN", csrfToken)
                .build();
    }
}
