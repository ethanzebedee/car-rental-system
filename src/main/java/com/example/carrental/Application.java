package com.example.carrental;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		// entry point required by Spring Boot
		// Needed for the existing project structure
		// No web endpoints are implemented for the assessment but could be added later
		// for a real service.
		SpringApplication.run(Application.class, args);
	}

}

// NOTE: REST layer is out of scope for this assignment
// If added, controllers would use CarRentalService in the background.
