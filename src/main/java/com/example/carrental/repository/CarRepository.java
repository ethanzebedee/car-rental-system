package com.example.carrental.repository;

import com.example.carrental.domain.Car;
import com.example.carrental.domain.CarType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CarRepository extends JpaRepository<Car, String> {

    List<Car> findByType(CarType type);

    /**
     * Fetches all cars of the given type while acquiring a pessimistic write
     * lock on each row. This prevents concurrent transactions from reading the
     * same rows until the current transaction commits, eliminating the
     * check-then-act race condition in {@code reserveCar}.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Car c WHERE c.type = :type")
    List<Car> findByTypeWithLock(@Param("type") CarType type);

    long countByType(CarType type);
}