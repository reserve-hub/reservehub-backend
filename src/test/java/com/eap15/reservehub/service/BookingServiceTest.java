package com.eap15.reservehub.service;

import com.eap15.reservehub.dto.BookingRequestDTO;
import com.eap15.reservehub.dto.BookingResponseDTO;
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

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BookingService bookingService;

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

    // HU-08 Escenario 1: Creación exitosa
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
        // Verifica que se descontó un cupo
        assertThat(schedule.getAvailableSlots()).isEqualTo(2);
        verify(scheduleRepository).save(schedule);
    }

    // HU-08 Escenario 2: Sin cupos disponibles
    @Test
    void createBooking_noSlots_throws() {
        schedule.setAvailableSlots(0);
        when(userRepository.findById(1L)).thenReturn(Optional.of(client));
        when(scheduleRepository.findById(5L)).thenReturn(Optional.of(schedule));

        assertThatThrownBy(() -> bookingService.createBooking(1L, requestDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cupos disponibles");
    }

    // HU-08 Escenario 3: Franja inactiva
    @Test
    void createBooking_inactiveSchedule_throws() {
        schedule.setActive(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(client));
        when(scheduleRepository.findById(5L)).thenReturn(Optional.of(schedule));

        assertThatThrownBy(() -> bookingService.createBooking(1L, requestDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no existe o no está disponible");
    }

    // HU-08 Escenario 3: Franja inexistente
    @Test
    void createBooking_scheduleNotFound_throws() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(client));
        when(scheduleRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.createBooking(1L, requestDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no existe");
    }

    // HU-08 Escenario 5: Cupo se decrementa correctamente
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
}
