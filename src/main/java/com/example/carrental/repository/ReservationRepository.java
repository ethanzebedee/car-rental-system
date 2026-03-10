package com.example.carrental.repository;

import com.example.carrental.domain.Reservation;
import com.example.carrental.domain.CarType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, String> {

    List<Reservation> findByCarType(CarType carType);
}