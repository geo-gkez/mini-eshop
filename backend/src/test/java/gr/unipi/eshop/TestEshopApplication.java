package gr.unipi.eshop;

import org.springframework.boot.SpringApplication;

public class TestEshopApplication {

	public static void main(String[] args) {
		SpringApplication.from(EshopApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
