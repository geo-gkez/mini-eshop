package gr.unipi.eshop;

import gr.unipi.eshop.auth.DataInitializer;
import org.springframework.boot.SpringApplication;

public class TestEshopApplication {

    static void main(String[] args) {
        SpringApplication.from(EshopApplication::main).with(TestcontainersConfiguration.class, DataInitializer.class).run(args);
    }

}
