package com.eap15.reservehub.service;

import com.eap15.reservehub.dto.BookingReportDTO;
import com.eap15.reservehub.dto.BookingRequestDTO;
import com.eap15.reservehub.dto.BookingResponseDTO;
import com.eap15.reservehub.dto.RescheduleRequestDTO;
import com.eap15.reservehub.entity.Booking;
import com.eap15.reservehub.entity.Schedule;
import com.eap15.reservehub.entity.User;
import com.eap15.reservehub.repository.BookingRepository;
import com.eap15.reservehub.repository.ScheduleRepository;
import com.eap15.reservehub.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private ScheduleRepository scheduleRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private BookingService bookingService;

    private User client;
    private User provider;
    private Schedule schedule;
    private BookingRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        provider = new User();
        provider.setId(2L);
        provider.setFirstName("Ana");
        provider.setLastName("García");
        provider.setRole(User.Role.PROVEEDOR);
        provider.setServiceType("Estética");

        client = new User();
        client.setId(1L);
        client.setFirstName("Juan");
        client.setLastName("Pérez");
        client.setRole(User.Role.CLIENTE);

        schedule = new Schedule();
        schedule.setId(5L);
        schedule.setProvider(provider);
        schedule.setStartTime(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0));
        schedule.setEndTime(LocalDateTime.now().plusDays(1).withHour(11).withMinute(0));
        schedule.setAvailableSlots(3);
        schedule.setActive(true);
        schedule.setCreatedAt(LocalDateTime.now());

        requestDTO = new BookingRequestDTO();
        requestDTO.setScheduleId(5L);
    }

    // ═══════════════════════════════════════════════════════════════
    //  Sprint 2 — HU-08: Creación de reservas
    // ═══════════════════════════════════════════════════════════════

    @Test
    void createBooking_successful() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(client));
        when(scheduleRepository.findById(5L)).thenReturn(Optional.of(schedule));
        when(scheduleRepository.save(any())).thenReturn(schedule);

        Booking saved = new Booking();
        saved.setId(100L);
        saved.setClient(client);
        saved.setSchedule(schedule);
        saved.setStatus(Booking.BookingStatus.CONFIRMED);
        saved.setCreatedAt(LocalDateTime.now());
        when(bookingRepository.save(any(Booking.class))).thenReturn(saved);

        BookingResponseDTO result = bookingService.createBooking(1L, requestDTO);

        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getStatus()).isEqualTo(Booking.BookingStatus.CONFIRMED);
        assertThat(schedule.getAvailableSlots()).isEqualTo(2);
        verify(scheduleRepository).save(schedule);
    }

    @Test
    void createBooking_noSlots_throws() {
        schedule.setAvailableSlots(0);
        when(userRepository.findById(1L)).thenReturn(Optional.of(client));
        when(scheduleRepository.findById(5L)).thenReturn(Optional.of(schedule));

        assertThatThrownBy(() -> bookingService.createBooking(1L, requestDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cupos disponibles");
    }

    @Test
    void createBooking_inactiveSchedule_throws() {
        schedule.setActive(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(client));
        when(scheduleRepository.findById(5L)).thenReturn(Optional.of(schedule));

        assertThatThrownBy(() -> bookingService.createBooking(1L, requestDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no existe o no está disponible");
    }

    @Test
    void createBooking_scheduleNotFound_throws() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(client));
        when(scheduleRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.createBooking(1L, requestDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no existe");
    }

    @Test
    void createBooking_decrementsSlot() {
        schedule.setAvailableSlots(1);
        when(userRepository.findById(1L)).thenReturn(Optional.of(client));
        when(scheduleRepository.findById(5L)).thenReturn(Optional.of(schedule));
        when(scheduleRepository.save(any())).thenReturn(schedule);

        Booking saved = new Booking();
        saved.setId(101L);
        saved.setClient(client);
        saved.setSchedule(schedule);
        saved.setStatus(Booking.BookingStatus.CONFIRMED);
        saved.setCreatedAt(LocalDateTime.now());
        when(bookingRepository.save(any())).thenReturn(saved);

        bookingService.createBooking(1L, requestDTO);

        assertThat(schedule.getAvailableSlots()).isEqualTo(0);
    }

    @Test
    void createBooking_clientNotFound_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        BookingRequestDTO dto = new BookingRequestDTO();
        dto.setScheduleId(5L);

        assertThatThrownBy(() -> bookingService.createBooking(99L, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cliente no encontrado");
    }

    @Test
    void getMyBookings_returnsClientBookings() {
        Booking booking = new Booking();
        booking.setId(200L);
        booking.setClient(client);
        booking.setSchedule(schedule);
        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        booking.setCreatedAt(LocalDateTime.now());

        when(bookingRepository.findByClientId(1L)).thenReturn(List.of(booking));

        List<BookingResponseDTO> result = bookingService.getMyBookings(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(Booking.BookingStatus.CONFIRMED);
    }

    @Test
    void getMyBookings_noBookings_returnsEmptyList() {
        when(bookingRepository.findByClientId(1L)).thenReturn(List.of());

        List<BookingResponseDTO> result = bookingService.getMyBookings(1L);

        assertThat(result).isEmpty();
    }

    // ═══════════════════════════════════════════════════════════════
    //  Sprint 3 — HU-10: Cancelación
    // ═══════════════════════════════════════════════════════════════

    // HU-10 Escenario 1: Cancelación exitosa
    @Test
    void cancelBooking_successful() {
        Booking booking = confirmedBooking(10L, client, schedule);
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));
        when(scheduleRepository.save(any())).thenReturn(schedule);

        Booking cancelled = confirmedBooking(10L, client, schedule);
        cancelled.setStatus(Booking.BookingStatus.CANCELLED);
        cancelled.setCancelledAt(LocalDateTime.now());
        when(bookingRepository.save(any())).thenReturn(cancelled);

        BookingResponseDTO result = bookingService.cancelBooking(10L, 1L);

        assertThat(result.getStatus()).isEqualTo(Booking.BookingStatus.CANCELLED);
        // El cupo debe haberse liberado
        assertThat(schedule.getAvailableSlots()).isEqualTo(4);
        verify(scheduleRepository).save(schedule);
    }

    // HU-10 Escenario 2: Reserva no encontrada
    @Test
    void cancelBooking_notFound_throws() {
        when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.cancelBooking(999L, 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Reserva no encontrada");
    }

    // HU-10 Escenario 3: Usuario no es el propietario
    @Test
    void cancelBooking_notOwner_throwsAccessDenied() {
        Booking booking = confirmedBooking(10L, client, schedule);
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));

        // clientId = 99 (otro usuario)
        assertThatThrownBy(() -> bookingService.cancelBooking(10L, 99L))
                .isInstanceOf(AccessDeniedException.class);
    }

    // Cancelar una reserva ya cancelada
    @Test
    void cancelBooking_alreadyCancelled_throws() {
        Booking booking = confirmedBooking(10L, client, schedule);
        booking.setStatus(Booking.BookingStatus.CANCELLED);
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancelBooking(10L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya se encuentra cancelada");
    }

    // ═══════════════════════════════════════════════════════════════
    //  Sprint 3 — HU-10: Reagendamiento
    // ═══════════════════════════════════════════════════════════════

    // HU-10 Escenario 4: Reagendamiento exitoso
    @Test
    void rescheduleBooking_successful() {
        Schedule newSchedule = new Schedule();
        newSchedule.setId(20L);
        newSchedule.setProvider(provider);
        newSchedule.setStartTime(LocalDateTime.now().plusDays(2).withHour(14).withMinute(0));
        newSchedule.setEndTime(LocalDateTime.now().plusDays(2).withHour(15).withMinute(0));
        newSchedule.setAvailableSlots(2);
        newSchedule.setActive(true);
        newSchedule.setCreatedAt(LocalDateTime.now());

        Booking booking = confirmedBooking(10L, client, schedule);
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));
        when(scheduleRepository.findById(20L)).thenReturn(Optional.of(newSchedule));
        when(scheduleRepository.save(any())).thenReturn(schedule);

        Booking rescheduled = new Booking();
        rescheduled.setId(10L);
        rescheduled.setClient(client);
        rescheduled.setSchedule(newSchedule);
        rescheduled.setStatus(Booking.BookingStatus.RESCHEDULED);
        rescheduled.setCreatedAt(LocalDateTime.now());
        rescheduled.setUpdatedAt(LocalDateTime.now());
        when(bookingRepository.save(any())).thenReturn(rescheduled);

        RescheduleRequestDTO dto = new RescheduleRequestDTO();
        dto.setNewScheduleId(20L);

        BookingResponseDTO result = bookingService.rescheduleBooking(10L, dto, 1L);

        assertThat(result.getStatus()).isEqualTo(Booking.BookingStatus.RESCHEDULED);
        // Cupo del horario original liberado (+1) y del nuevo descontado (-1)
        assertThat(schedule.getAvailableSlots()).isEqualTo(4);    // era 3, +1
        assertThat(newSchedule.getAvailableSlots()).isEqualTo(1); // era 2, -1
    }

    // HU-10 Escenario 5: Nuevo horario sin cupos
    @Test
    void rescheduleBooking_newScheduleNoSlots_throws() {
        Schedule newSchedule = new Schedule();
        newSchedule.setId(20L);
        newSchedule.setProvider(provider);
        newSchedule.setAvailableSlots(0);
        newSchedule.setActive(true);
        newSchedule.setCreatedAt(LocalDateTime.now());

        Booking booking = confirmedBooking(10L, client, schedule);
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));
        when(scheduleRepository.findById(20L)).thenReturn(Optional.of(newSchedule));

        RescheduleRequestDTO dto = new RescheduleRequestDTO();
        dto.setNewScheduleId(20L);

        assertThatThrownBy(() -> bookingService.rescheduleBooking(10L, dto, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no tiene cupos disponibles");
    }

    // HU-10 Escenario 6: Intentar reagendar una reserva cancelada
    @Test
    void rescheduleBooking_cancelledBooking_throws() {
        Booking booking = confirmedBooking(10L, client, schedule);
        booking.setStatus(Booking.BookingStatus.CANCELLED);
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));

        RescheduleRequestDTO dto = new RescheduleRequestDTO();
        dto.setNewScheduleId(20L);

        assertThatThrownBy(() -> bookingService.rescheduleBooking(10L, dto, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cancelada no puede ser modificada");
    }

    // HU-10: reagendar con propietario incorrecto
    @Test
    void rescheduleBooking_notOwner_throwsAccessDenied() {
        Booking booking = confirmedBooking(10L, client, schedule);
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));

        RescheduleRequestDTO dto = new RescheduleRequestDTO();
        dto.setNewScheduleId(20L);

        assertThatThrownBy(() -> bookingService.rescheduleBooking(10L, dto, 99L))
                .isInstanceOf(AccessDeniedException.class);
    }

    // HU-10: reagendar con nuevo horario inactivo
    @Test
    void rescheduleBooking_inactiveNewSchedule_throws() {
        Schedule newSchedule = new Schedule();
        newSchedule.setId(20L);
        newSchedule.setProvider(provider);
        newSchedule.setAvailableSlots(3);
        newSchedule.setActive(false);
        newSchedule.setCreatedAt(LocalDateTime.now());

        Booking booking = confirmedBooking(10L, client, schedule);
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));
        when(scheduleRepository.findById(20L)).thenReturn(Optional.of(newSchedule));

        RescheduleRequestDTO dto = new RescheduleRequestDTO();
        dto.setNewScheduleId(20L);

        assertThatThrownBy(() -> bookingService.rescheduleBooking(10L, dto, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no existe o no está disponible");
    }

    // ═══════════════════════════════════════════════════════════════
    //  Sprint 3 — HU-11: Historial con filtros
    // ═══════════════════════════════════════════════════════════════

    // HU-11 Escenario 1: historial completo sin filtros
    @Test
    void getMyBookingsFiltered_noFilters_returnsAll() {
        Booking b1 = confirmedBooking(1L, client, schedule);
        Booking b2 = confirmedBooking(2L, client, schedule);
        b2.setStatus(Booking.BookingStatus.CANCELLED);

        when(bookingRepository.findByClientId(1L)).thenReturn(List.of(b1, b2));

        List<BookingResponseDTO> result = bookingService.getMyBookingsFiltered(1L, null, null, null);

        assertThat(result).hasSize(2);
    }

    // HU-11 Escenario 2: sin reservas → lista vacía
    @Test
    void getMyBookingsFiltered_noBookings_returnsEmpty() {
        when(bookingRepository.findByClientId(1L)).thenReturn(List.of());

        List<BookingResponseDTO> result = bookingService.getMyBookingsFiltered(1L, null, null, null);

        assertThat(result).isEmpty();
    }

    // HU-11 Escenario 3: filtrar por estado
    @Test
    void getMyBookingsFiltered_byStatus_returnsCancelled() {
        Booking cancelled = confirmedBooking(1L, client, schedule);
        cancelled.setStatus(Booking.BookingStatus.CANCELLED);

        when(bookingRepository.findByClientIdAndStatus(1L, Booking.BookingStatus.CANCELLED))
                .thenReturn(List.of(cancelled));

        List<BookingResponseDTO> result = bookingService.getMyBookingsFiltered(
                1L, Booking.BookingStatus.CANCELLED, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(Booking.BookingStatus.CANCELLED);
    }

    // HU-11 Escenario 4: filtrar por rango de fechas
    @Test
    void getMyBookingsFiltered_byDateRange_returnsFiltered() {
        LocalDateTime from = LocalDateTime.now().minusDays(5);
        LocalDateTime to = LocalDateTime.now();
        Booking booking = confirmedBooking(1L, client, schedule);

        when(bookingRepository.findByClientIdAndCreatedAtBetween(1L, from, to))
                .thenReturn(List.of(booking));

        List<BookingResponseDTO> result = bookingService.getMyBookingsFiltered(1L, null, from, to);

        assertThat(result).hasSize(1);
    }

    // HU-11 Escenario 4: rango de fechas inválido
    @Test
    void getMyBookingsFiltered_invalidDateRange_throws() {
        LocalDateTime from = LocalDateTime.now();
        LocalDateTime to = LocalDateTime.now().minusDays(1);

        assertThatThrownBy(() -> bookingService.getMyBookingsFiltered(1L, null, from, to))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rango de fechas no es válido");
    }

    // HU-11 Escenario 6: proveedor consulta sus reservas
    @Test
    void getBookingsByProvider_returnsProviderBookings() {
        Booking booking = confirmedBooking(1L, client, schedule);

        when(bookingRepository.findByScheduleProviderId(2L)).thenReturn(List.of(booking));

        List<BookingResponseDTO> result = bookingService.getBookingsByProvider(2L, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProviderName()).isEqualTo("Ana García");
    }

    // ═══════════════════════════════════════════════════════════════
    //  Sprint 3 — HU-12: Reportes operativos
    // ═══════════════════════════════════════════════════════════════

    // HU-12 Escenario 1: reporte admin sin filtro de fechas
    @Test
    void getAdminReport_noDateFilter_returnsFullReport() {
        Booking b1 = confirmedBooking(1L, client, schedule);
        Booking b2 = confirmedBooking(2L, client, schedule);
        b2.setStatus(Booking.BookingStatus.CANCELLED);
        Booking b3 = confirmedBooking(3L, client, schedule);
        b3.setStatus(Booking.BookingStatus.RESCHEDULED);

        when(bookingRepository.findAll()).thenReturn(List.of(b1, b2, b3));

        BookingReportDTO report = bookingService.getAdminReport(null, null);

        assertThat(report.getTotal()).isEqualTo(3);
        assertThat(report.getConfirmed()).isEqualTo(1);
        assertThat(report.getCancelled()).isEqualTo(1);
        assertThat(report.getRescheduled()).isEqualTo(1);
        assertThat(report.getDateFrom()).isNull();
        assertThat(report.getDateTo()).isNull();
    }

    // HU-12 Escenario 4: sin datos en el periodo
    @Test
    void getAdminReport_noDataInRange_returnsZeroes() {
        LocalDateTime from = LocalDateTime.now().minusDays(30);
        LocalDateTime to = LocalDateTime.now().minusDays(20);

        when(bookingRepository.findAllByDateRange(from, to)).thenReturn(List.of());

        BookingReportDTO report = bookingService.getAdminReport(from, to);

        assertThat(report.getTotal()).isEqualTo(0);
        assertThat(report.getConfirmed()).isEqualTo(0);
    }

    // HU-12 Escenario 6: rango de fechas inválido en reporte
    @Test
    void getAdminReport_invalidDateRange_throws() {
        LocalDateTime from = LocalDateTime.now();
        LocalDateTime to = LocalDateTime.now().minusDays(1);

        assertThatThrownBy(() -> bookingService.getAdminReport(from, to))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rango de fechas no es válido");
    }

    // HU-12 Escenario 2: reporte del proveedor
    @Test
    void getProviderReport_returnsProviderStats() {
        Booking b1 = confirmedBooking(1L, client, schedule);
        Booking b2 = confirmedBooking(2L, client, schedule);
        b2.setStatus(Booking.BookingStatus.CANCELLED);

        when(bookingRepository.findByScheduleProviderId(2L)).thenReturn(List.of(b1, b2));
        when(scheduleRepository.findByProviderIdAndActiveTrue(2L)).thenReturn(List.of(schedule));

        BookingReportDTO report = bookingService.getProviderReport(2L, null, null);

        assertThat(report.getTotal()).isEqualTo(2);
        assertThat(report.getConfirmed()).isEqualTo(1);
        assertThat(report.getCancelled()).isEqualTo(1);
        assertThat(report.getOccupancy()).isNotNull().hasSize(1);
    }

    // HU-12 Escenario 3: tasa de ocupación correcta
    @Test
    void getProviderReport_occupancyRateIsCorrect() {
        // schedule tiene 3 cupos disponibles, 1 reserva activa → total=4, rate=25%
        Booking b1 = confirmedBooking(1L, client, schedule);

        when(bookingRepository.findByScheduleProviderId(2L)).thenReturn(List.of(b1));
        when(scheduleRepository.findByProviderIdAndActiveTrue(2L)).thenReturn(List.of(schedule));

        BookingReportDTO report = bookingService.getProviderReport(2L, null, null);

        // 1 used / (1 used + 3 available) = 25%
        assertThat(report.getOccupancy().get(0).getOccupancyRate()).isEqualTo(25.0);
        assertThat(report.getOccupancy().get(0).getTotalSlots()).isEqualTo(4);
        assertThat(report.getOccupancy().get(0).getUsedSlots()).isEqualTo(1);
    }

    // ═══════════════════════════════════════════════════════════════
    //  Helper
    // ═══════════════════════════════════════════════════════════════

    private Booking confirmedBooking(Long id, User client, Schedule schedule) {
        Booking b = new Booking();
        b.setId(id);
        b.setClient(client);
        b.setSchedule(schedule);
        b.setStatus(Booking.BookingStatus.CONFIRMED);
        b.setCreatedAt(LocalDateTime.now());
        return b;
    }
}
