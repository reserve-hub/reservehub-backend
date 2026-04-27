package com.eap15.reservehub.repository;

import com.eap15.reservehub.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    List<Schedule> findByProviderIdAndActiveTrue(Long providerId);

    List<Schedule> findByActiveTrue();

    @Query("SELECT s FROM Schedule s WHERE s.provider.id = :providerId " +
           "AND s.active = true " +
           "AND s.startTime < :endTime AND s.endTime > :startTime")
    List<Schedule> findOverlapping(
            @Param("providerId") Long providerId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    @Query("SELECT s FROM Schedule s WHERE s.active = true AND s.availableSlots > 0 " +
           "AND (:providerId IS NULL OR s.provider.id = :providerId) " +
           "AND (:serviceType IS NULL OR LOWER(s.provider.serviceType) LIKE LOWER(CONCAT('%', :serviceType, '%'))) " +
           "AND (:date IS NULL OR CAST(s.startTime AS date) = :date)")
    List<Schedule> findAvailable(
            @Param("providerId") Long providerId,
            @Param("serviceType") String serviceType,
            @Param("date") java.time.LocalDate date);
}
