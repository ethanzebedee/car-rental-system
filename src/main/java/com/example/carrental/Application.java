package com.example.carrental;

import com.example.carrental.domain.CarType;
import com.example.carrental.repository.CarRepository;
import com.example.carrental.repository.ReservationRepository;
import com.example.carrental.service.CarRentalService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Bean
	CarRentalService carRentalService(CarRepository carRepository, ReservationRepository reservationRepository) {
		return new CarRentalService(carRepository, reservationRepository);
	}

}
