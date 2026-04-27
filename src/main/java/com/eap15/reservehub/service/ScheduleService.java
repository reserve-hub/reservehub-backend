package com.eap15.reservehub.service;

import com.eap15.reservehub.dto.ScheduleRequestDTO;
import com.eap15.reservehub.dto.ScheduleResponseDTO;
import com.eap15.reservehub.entity.Schedule;
import com.eap15.reservehub.entity.User;
import com.eap15.reservehub.repository.ScheduleRepository;
import com.eap15.reservehub.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class ScheduleService {

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private UserRepository userRepository;

    // HU-06 Escenario 1: Registro exitoso de una franja
    @Transactional
    public ScheduleResponseDTO createSchedule(Long providerId, ScheduleRequestDTO dto) {
        User provider = userRepository.findById(providerId)
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado con ID: " + providerId));

        if (provider.getRole() != User.Role.PROVEEDOR) {
            throw new IllegalArgumentException("Solo los proveedores pueden registrar franjas horarias");
        }

        // HU-06 Escenario 2: Rango horario inválido
        if (dto.getEndTime() == null || dto.getStartTime() == null
                || !dto.getEndTime().isAfter(dto.getStartTime())) {
            throw new IllegalArgumentException("El rango horario no es válido: la hora de fin debe ser posterior a la de inicio");
        }

        // HU-06 Escenario 3: Franja traslapada
        List<Schedule> overlapping = scheduleRepository.findOverlapping(
                providerId, dto.getStartTime(), dto.getEndTime());
        if (!overlapping.isEmpty()) {
            throw new IllegalArgumentException("Existe traslape con otra franja horaria registrada");
        }

        Schedule schedule = new Schedule();
        schedule.setProvider(provider);
        schedule.setStartTime(dto.getStartTime());
        schedule.setEndTime(dto.getEndTime());
        schedule.setAvailableSlots(dto.getAvailableSlots());
        schedule.setActive(true);

        return toResponseDTO(scheduleRepository.save(schedule));
    }

    // HU-07: Consultar disponibilidad con filtros opcionales
    public List<ScheduleResponseDTO> getAvailableSchedules(Long providerId, String serviceType, LocalDate date) {
        List<Schedule> schedules = scheduleRepository.findAvailable(providerId, serviceType, date);
        return schedules.stream().map(this::toResponseDTO).toList();
    }

    // HU-06: Obtener franjas propias del proveedor
    public List<ScheduleResponseDTO> getMySchedules(Long providerId) {
        return scheduleRepository.findByProviderIdAndActiveTrue(providerId)
                .stream().map(this::toResponseDTO).toList();
    }

    // HU-06 Escenario 5: Activar o desactivar franja
    @Transactional
    public ScheduleResponseDTO toggleScheduleStatus(Long scheduleId, Long providerId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Franja horaria no encontrada con ID: " + scheduleId));

        if (!schedule.getProvider().getId().equals(providerId)) {
            throw new IllegalArgumentException("No tiene permiso para modificar esta franja");
        }

        schedule.setActive(!schedule.isActive());
        return toResponseDTO(scheduleRepository.save(schedule));
    }

    private ScheduleResponseDTO toResponseDTO(Schedule s) {
        ScheduleResponseDTO dto = new ScheduleResponseDTO();
        dto.setId(s.getId());
        dto.setProviderId(s.getProvider().getId());
        dto.setProviderName(s.getProvider().getFirstName() + " " + s.getProvider().getLastName());
        dto.setServiceType(s.getProvider().getServiceType());
        dto.setStartTime(s.getStartTime());
        dto.setEndTime(s.getEndTime());
        dto.setAvailableSlots(s.getAvailableSlots());
        dto.setActive(s.isActive());
        dto.setCreatedAt(s.getCreatedAt());
        return dto;
    }
}
