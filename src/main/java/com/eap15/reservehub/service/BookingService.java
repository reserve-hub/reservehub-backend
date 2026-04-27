package com.eap15.reservehub.service;

import com.eap15.reservehub.dto.BookingRequestDTO;
import com.eap15.reservehub.dto.BookingResponseDTO;
import com.eap15.reservehub.entity.Booking;
import com.eap15.reservehub.entity.Schedule;
import com.eap15.reservehub.entity.User;
import com.eap15.reservehub.repository.BookingRepository;
import com.eap15.reservehub.repository.ScheduleRepository;
import com.eap15.reservehub.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private UserRepository userRepository;

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

    // Consultar reservas del cliente autenticado
    public List<BookingResponseDTO> getMyBookings(Long clientId) {
        return bookingRepository.findByClientId(clientId)
                .stream().map(this::toResponseDTO).toList();
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
        return dto;
    }
}
