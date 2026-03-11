package com.example.carrental.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.carrental.domain.CarType;
import com.example.carrental.domain.Reservation;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, String> {

    List<Reservation> findByCarType(CarType carType);

    List<Reservation> findByCarId(String carId);

    List<Reservation> findByCarIdIn(List<String> carIds);

    @Modifying
    @Query("DELETE FROM Reservation r WHERE r.id = :id")
    int deleteReservationById(@Param("id") String id);
}