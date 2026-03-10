package com.example.carrental.controller;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.example.carrental.domain.CarType;
import com.example.carrental.domain.Reservation;
import com.example.carrental.service.CarRentalService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/reservations")
@Tag(name = "Reservation Management", description = "APIs for managing car reservations")
public class ReservationController {

    private final CarRentalService carRentalService;

    public ReservationController(CarRentalService carRentalService) {
        this.carRentalService = carRentalService;
    }

    @PostMapping
    @Operation(summary = "Create a new reservation", description = "Books the first available car of the given type for the requested dates")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Reservation created"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @ApiResponse(responseCode = "409", description = "No car available for the requested dates")
    })
    public ResponseEntity<Reservation> createReservation(
            @Parameter(description = "Type of car (SEDAN, SUV, VAN)") @RequestParam CarType carType,
            @Parameter(description = "Rental start date (ISO-8601, e.g. 2026-06-01T10:00:00)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "Number of rental days (must be > 0)") @RequestParam int days) {

        Reservation reservation = carRentalService.reserveCar(carType, startDate, days);
        URI location = ServletUriComponentsBuilder.fromCurrentServletMapping()
                .path("/api/reservations/{id}")
                .buildAndExpand(reservation.getId())
                .toUri();
        return ResponseEntity.created(location).body(reservation);
    }

    @GetMapping
    @Operation(summary = "List all reservations")
    @ApiResponse(responseCode = "200", description = "List of all reservations")
    public ResponseEntity<List<Reservation>> getAllReservations() {
        return ResponseEntity.ok(carRentalService.getAllReservations());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get reservation by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reservation found"),
            @ApiResponse(responseCode = "404", description = "Reservation not found")
    })
    public ResponseEntity<Reservation> getReservation(@PathVariable String id) {
        return carRentalService.getReservationById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel a reservation")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Reservation cancelled"),
            @ApiResponse(responseCode = "404", description = "Reservation not found")
    })
    public ResponseEntity<Void> cancelReservation(@PathVariable String id) {
        boolean cancelled = carRentalService.cancelReservation(id);
        return cancelled ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @GetMapping("/availability")
    @Operation(summary = "Check car availability", description = "Returns how many cars of the given type are free for the requested period")
    @ApiResponse(responseCode = "200", description = "Availability information")
    public ResponseEntity<Map<String, Object>> checkAvailability(
            @Parameter(description = "Type of car (SEDAN, SUV, VAN)") @RequestParam CarType type,
            @Parameter(description = "Rental start date (ISO-8601)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "Number of rental days (> 0)") @RequestParam int days) {

        if (days <= 0) {
            throw new IllegalArgumentException("days must be greater than 0");
        }
        int availableCount = carRentalService.getAvailableCarsCount(type, startDate, days);
        int totalCount = carRentalService.getTotalCarsCount(type);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("carType", type);
        response.put("startDate", startDate);
        response.put("days", days);
        response.put("availableCount", availableCount);
        response.put("totalCount", totalCount);
        response.put("available", availableCount > 0);

        return ResponseEntity.ok(response);
    }
}