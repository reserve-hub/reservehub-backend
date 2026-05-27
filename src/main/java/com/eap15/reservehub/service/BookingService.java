package com.eap15.reservehub.service;

import com.eap15.reservehub.dto.BookingReportDTO;
import com.eap15.reservehub.dto.BookingRequestDTO;
import com.eap15.reservehub.dto.BookingResponseDTO;
import com.eap15.reservehub.dto.RescheduleRequestDTO;
import com.eap15.reservehub.dto.ScheduleOccupancyDTO;
import com.eap15.reservehub.entity.Booking;
import com.eap15.reservehub.entity.Schedule;
import com.eap15.reservehub.entity.User;
import com.eap15.reservehub.repository.BookingRepository;
import com.eap15.reservehub.repository.ScheduleRepository;
import com.eap15.reservehub.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ScheduleRepository scheduleRepository;
    private final UserRepository userRepository;

    public BookingService(BookingRepository bookingRepository,
                          ScheduleRepository scheduleRepository,
                          UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.scheduleRepository = scheduleRepository;
        this.userRepository = userRepository;
    }

    // ── Sprint 2 ──────────────────────────────────────────────────────────────

    // HU-08 Escenario 1: Creación exitosa de reserva
    @Transactional
    public BookingResponseDTO createBooking(Long clientId, BookingRequestDTO dto) {
        User client = userRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado con ID: " + clientId));

        // HU-08 Escenario 3: Franja inexistente o inactiva
        Schedule schedule = scheduleRepository.findById(dto.getScheduleId())
                .orElseThrow(() -> new IllegalArgumentException("La franja horaria no existe o no está disponible"));

        if (!schedule.isActive()) {
            throw new IllegalArgumentException("La franja horaria no existe o no está disponible");
        }

        // HU-08 Escenario 2: Sin cupos
        if (schedule.getAvailableSlots() <= 0) {
            throw new IllegalArgumentException("El horario seleccionado ya no tiene cupos disponibles");
        }

        // HU-08 Escenario 1 + 5: Descontar cupo
        schedule.setAvailableSlots(schedule.getAvailableSlots() - 1);
        scheduleRepository.save(schedule);

        Booking booking = new Booking();
        booking.setClient(client);
        booking.setSchedule(schedule);
        booking.setStatus(Booking.BookingStatus.CONFIRMED);

        return toResponseDTO(bookingRepository.save(booking));
    }

    // HU-08 / HU-11 Escenario 1: Consultar mis reservas (sin filtros — delegación)
    public List<BookingResponseDTO> getMyBookings(Long clientId) {
        return getMyBookingsFiltered(clientId, null, null, null);
    }

    // ── Sprint 3 — HU-10: Cancelación y reagendamiento ───────────────────────

    /**
     * HU-10 Escenarios 1-3: Cancelar una reserva.
     * Solo el propietario puede cancelar. La franja recupera el cupo.
     */
    @Transactional
    public BookingResponseDTO cancelBooking(Long bookingId, Long clientId) {
        // HU-10 Escenario 2: reserva no encontrada
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada con ID: " + bookingId));

        // HU-10 Escenario 3: el cliente no es el propietario
        if (!booking.getClient().getId().equals(clientId)) {
            throw new AccessDeniedException("No tienes permiso para cancelar esta reserva");
        }

        // Solo se puede cancelar si está activa o reagendada
        if (booking.getStatus() == Booking.BookingStatus.CANCELLED) {
            throw new IllegalArgumentException("La reserva ya se encuentra cancelada");
        }

        // Liberar el cupo de la franja horaria
        Schedule schedule = booking.getSchedule();
        schedule.setAvailableSlots(schedule.getAvailableSlots() + 1);
        scheduleRepository.save(schedule);

        booking.setStatus(Booking.BookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());

        return toResponseDTO(bookingRepository.save(booking));
    }

    /**
     * HU-10 Escenarios 4-6: Reagendar una reserva.
     * Solo el propietario puede reagendar. El cupo antiguo se libera y el nuevo se descuenta.
     */
    @Transactional
    public BookingResponseDTO rescheduleBooking(Long bookingId, RescheduleRequestDTO dto, Long clientId) {
        // HU-10 Escenario 2 (reutilizado): reserva no encontrada
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada con ID: " + bookingId));

        // HU-10 Escenario 3: cliente no es el propietario
        if (!booking.getClient().getId().equals(clientId)) {
            throw new AccessDeniedException("No tienes permiso para reagendar esta reserva");
        }

        // HU-10 Escenario 6: reserva ya cancelada
        if (booking.getStatus() == Booking.BookingStatus.CANCELLED) {
            throw new IllegalArgumentException("Una reserva cancelada no puede ser modificada");
        }

        // HU-10 Escenario 5: nuevo horario sin cupos o inactivo
        Schedule newSchedule = scheduleRepository.findById(dto.getNewScheduleId())
                .orElseThrow(() -> new IllegalArgumentException("El nuevo horario no existe o no está disponible"));

        if (!newSchedule.isActive()) {
            throw new IllegalArgumentException("El nuevo horario no existe o no está disponible");
        }

        if (newSchedule.getAvailableSlots() <= 0) {
            throw new IllegalArgumentException("El nuevo horario no tiene cupos disponibles");
        }

        // Liberar cupo del horario anterior
        Schedule oldSchedule = booking.getSchedule();
        oldSchedule.setAvailableSlots(oldSchedule.getAvailableSlots() + 1);
        scheduleRepository.save(oldSchedule);

        // Descontar cupo del nuevo horario
        newSchedule.setAvailableSlots(newSchedule.getAvailableSlots() - 1);
        scheduleRepository.save(newSchedule);

        // Actualizar la reserva
        booking.setSchedule(newSchedule);
        booking.setStatus(Booking.BookingStatus.RESCHEDULED);
        booking.setUpdatedAt(LocalDateTime.now());

        return toResponseDTO(bookingRepository.save(booking));
    }

    // ── Sprint 3 — HU-11: Historial con filtros ──────────────────────────────

    /**
     * HU-11 Escenarios 1-5: Historial del cliente con filtros opcionales.
     * Valida que el rango de fechas sea coherente (HU-11 Escenario 4).
     */
    public List<BookingResponseDTO> getMyBookingsFiltered(
            Long clientId,
            Booking.BookingStatus status,
            LocalDateTime from,
            LocalDateTime to) {

        validateDateRange(from, to);

        List<Booking> bookings;

        if (status != null && from != null && to != null) {
            bookings = bookingRepository.findByClientIdAndStatusAndCreatedAtBetween(clientId, status, from, to);
        } else if (status != null) {
            bookings = bookingRepository.findByClientIdAndStatus(clientId, status);
        } else if (from != null && to != null) {
            bookings = bookingRepository.findByClientIdAndCreatedAtBetween(clientId, from, to);
        } else {
            bookings = bookingRepository.findByClientId(clientId);
        }

        return bookings.stream().map(this::toResponseDTO).toList();
    }

    /**
     * HU-11 Escenario 6: El proveedor consulta reservas asociadas a sus servicios/agenda.
     */
    public List<BookingResponseDTO> getBookingsByProvider(Long providerId,
                                                          LocalDateTime from,
                                                          LocalDateTime to) {
        validateDateRange(from, to);

        List<Booking> bookings;
        if (from != null && to != null) {
            bookings = bookingRepository.findByScheduleProviderIdAndDateRange(providerId, from, to);
        } else {
            bookings = bookingRepository.findByScheduleProviderId(providerId);
        }

        return bookings.stream().map(this::toResponseDTO).toList();
    }

    // ── Sprint 3 — HU-12: Reportes operativos ────────────────────────────────

    /**
     * HU-12 Escenario 1: Reporte general para administrador.
     */
    public BookingReportDTO getAdminReport(LocalDateTime from, LocalDateTime to) {
        validateDateRange(from, to);

        List<Booking> bookings;
        if (from != null && to != null) {
            bookings = bookingRepository.findAllByDateRange(from, to);
        } else {
            bookings = bookingRepository.findAll();
        }

        return buildReport(bookings, from, to);
    }

    /**
     * HU-12 Escenarios 2-3: Reporte para proveedor (solo sus servicios + ocupación).
     */
    public BookingReportDTO getProviderReport(Long providerId, LocalDateTime from, LocalDateTime to) {
        validateDateRange(from, to);

        List<Booking> bookings;
        if (from != null && to != null) {
            bookings = bookingRepository.findByScheduleProviderIdAndDateRange(providerId, from, to);
        } else {
            bookings = bookingRepository.findByScheduleProviderId(providerId);
        }

        BookingReportDTO report = buildReport(bookings, from, to);

        // HU-12 Escenario 3: calcular ocupación por franja horaria
        List<Schedule> schedules = scheduleRepository.findByProviderIdAndActiveTrue(providerId);
        List<ScheduleOccupancyDTO> occupancy = schedules.stream()
                .map(s -> buildOccupancy(s, bookings))
                .toList();
        report.setOccupancy(occupancy);

        return report;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private BookingReportDTO buildReport(List<Booking> bookings, LocalDateTime from, LocalDateTime to) {
        Map<Booking.BookingStatus, Long> counts = bookings.stream()
                .collect(Collectors.groupingBy(Booking::getStatus, Collectors.counting()));

        BookingReportDTO report = new BookingReportDTO();
        report.setTotal(bookings.size());
        report.setConfirmed(counts.getOrDefault(Booking.BookingStatus.CONFIRMED, 0L));
        report.setCancelled(counts.getOrDefault(Booking.BookingStatus.CANCELLED, 0L));
        report.setRescheduled(counts.getOrDefault(Booking.BookingStatus.RESCHEDULED, 0L));
        report.setDateFrom(from);
        report.setDateTo(to);
        return report;
    }

    private ScheduleOccupancyDTO buildOccupancy(Schedule schedule, List<Booking> bookings) {
        long usedSlots = bookings.stream()
                .filter(b -> b.getSchedule().getId().equals(schedule.getId()))
                .filter(b -> b.getStatus() != Booking.BookingStatus.CANCELLED)
                .count();

        int available = schedule.getAvailableSlots();
        int total = (int) (usedSlots + available);
        double rate = total == 0 ? 0.0 : Math.round((double) usedSlots / total * 10000.0) / 100.0;

        ScheduleOccupancyDTO dto = new ScheduleOccupancyDTO();
        dto.setScheduleId(schedule.getId());
        dto.setStartTime(schedule.getStartTime());
        dto.setEndTime(schedule.getEndTime());
        dto.setTotalSlots(total);
        dto.setUsedSlots((int) usedSlots);
        dto.setAvailableSlots(available);
        dto.setOccupancyRate(rate);
        return dto;
    }

    private void validateDateRange(LocalDateTime from, LocalDateTime to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("El rango de fechas no es válido: 'from' debe ser anterior a 'to'");
        }
    }

    private BookingResponseDTO toResponseDTO(Booking b) {
        BookingResponseDTO dto = new BookingResponseDTO();
        dto.setId(b.getId());
        dto.setClientId(b.getClient().getId());
        dto.setClientName(b.getClient().getFirstName() + " " + b.getClient().getLastName());
        dto.setScheduleId(b.getSchedule().getId());
        dto.setScheduleStartTime(b.getSchedule().getStartTime());
        dto.setScheduleEndTime(b.getSchedule().getEndTime());
        dto.setProviderName(b.getSchedule().getProvider().getFirstName() + " " + b.getSchedule().getProvider().getLastName());
        dto.setServiceType(b.getSchedule().getProvider().getServiceType());
        dto.setStatus(b.getStatus());
        dto.setCreatedAt(b.getCreatedAt());
        dto.setCancelledAt(b.getCancelledAt());
        dto.setUpdatedAt(b.getUpdatedAt());
        return dto;
    }
}
