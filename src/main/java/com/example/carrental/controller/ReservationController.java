package com.example.carrental.controller;

import com.example.carrental.domain.CarType;
import com.example.carrental.domain.Reservation;
import com.example.carrental.service.CarRentalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reservations")
@Tag(name = "Reservation Management", description = "APIs for managing car reservations")
public class ReservationController {

    private final CarRentalService carRentalService;

    public ReservationController(CarRentalService carRentalService) {
        this.carRentalService = carRentalService;
    }

    @PostMapping
    @Operation(summary = "Create a new reservation")
    public ResponseEntity<Reservation> createReservation(
            @RequestParam CarType carType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam int days) {

        Reservation reservation = carRentalService.reserveCar(carType, startDate, days);
        return ResponseEntity.ok(reservation);
    }

    @GetMapping
    @Operation(summary = "Get all reservations")
    public ResponseEntity<List<Reservation>> getAllReservations() {
        List<Reservation> reservations = carRentalService.getAllReservations();
        return ResponseEntity.ok(reservations);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get reservation by ID")
    public ResponseEntity<Reservation> getReservation(@PathVariable String id) {
        return carRentalService.getAllReservations().stream()
                .filter(r -> r.getId().equals(id))
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel a reservation")
    public ResponseEntity<Void> cancelReservation(@PathVariable String id) {
        boolean cancelled = carRentalService.cancelReservation(id);
        return cancelled ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @GetMapping("/availability")
    @Operation(summary = "Check car availability")
    public ResponseEntity<Map<String, Object>> checkAvailability(
            @RequestParam CarType type,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam int days) {

        int availableCount = carRentalService.getAvailableCarsCount(type, startDate, days);
        int totalCount = carRentalService.getTotalCarsCount(type);

        Map<String, Object> response = Map.of(
                "carType", type,
                "startDate", startDate,
                "days", days,
                "availableCount", availableCount,
                "totalCount", totalCount);

        return ResponseEntity.ok(response);
    }
}