package com.example.carrental.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI carRentalOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Car Rental System API")
                        .description("""
                                RESTful API for managing car reservations, fleet inventory, and availability checking.

                                **Fleet**: 3 Sedans · 2 SUVs · 2 Vans (seeded at startup)

                                **Availability rule**: bookings are exclusive per car — no two reservations \
                                for the same car may overlap. Adjacent bookings (end == start) are allowed.
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Ethan Hammond")
                                .url("https://github.com/ethanzebedee/Car-Rental-System"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")));
    }
}
