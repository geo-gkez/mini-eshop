package gr.unipi.eshop;

import gr.unipi.eshop.auth.DataInitializer;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;


@Import({TestcontainersConfiguration.class, DataInitializer.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseIntegrationTest {

    @LocalServerPort
    private int port;

    protected RequestSpecification requestSpec;

    @BeforeEach
    void setUpBase() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        requestSpec = new RequestSpecBuilder()
                .setPort(port)
                .setContentType(ContentType.JSON)
                .build();
    }
}
