package com.eap15.reservehub.repository;

import com.eap15.reservehub.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // ── Sprint 2 ──────────────────────────────────────────────────────────────

    List<Booking> findByClientId(Long clientId);

    List<Booking> findByScheduleId(Long scheduleId);

    // ── Sprint 3 — HU-11: historial con filtros ───────────────────────────────

    /** HU-11 Escenario 3 — filtrar por estado */
    List<Booking> findByClientIdAndStatus(Long clientId, Booking.BookingStatus status);

    /** HU-11 Escenario 4 — filtrar por rango de fechas (sin filtro de estado) */
    List<Booking> findByClientIdAndCreatedAtBetween(
            Long clientId, LocalDateTime from, LocalDateTime to);

    /** HU-11 Escenarios 3+4 combinados */
    List<Booking> findByClientIdAndStatusAndCreatedAtBetween(
            Long clientId, Booking.BookingStatus status,
            LocalDateTime from, LocalDateTime to);

    // ── Sprint 3 — HU-11 Escenario 6: reservas del proveedor ─────────────────

    @Query("SELECT b FROM Booking b WHERE b.schedule.provider.id = :providerId")
    List<Booking> findByScheduleProviderId(@Param("providerId") Long providerId);

    @Query("SELECT b FROM Booking b WHERE b.schedule.provider.id = :providerId " +
           "AND b.createdAt BETWEEN :from AND :to")
    List<Booking> findByScheduleProviderIdAndDateRange(
            @Param("providerId") Long providerId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    // ── Sprint 3 — HU-12: reportes admin ─────────────────────────────────────

    @Query("SELECT b FROM Booking b WHERE b.createdAt BETWEEN :from AND :to")
    List<Booking> findAllByDateRange(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}
