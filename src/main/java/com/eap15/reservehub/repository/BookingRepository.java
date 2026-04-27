package com.eap15.reservehub.repository;

import com.eap15.reservehub.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByClientId(Long clientId);

    List<Booking> findByScheduleId(Long scheduleId);
}
