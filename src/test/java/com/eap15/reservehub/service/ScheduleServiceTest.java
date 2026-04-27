package com.eap15.reservehub.service;

import com.eap15.reservehub.dto.ScheduleRequestDTO;
import com.eap15.reservehub.dto.ScheduleResponseDTO;
import com.eap15.reservehub.entity.Schedule;
import com.eap15.reservehub.entity.User;
import com.eap15.reservehub.repository.ScheduleRepository;
import com.eap15.reservehub.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ScheduleService scheduleService;

    private User provider;
    private ScheduleRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        provider = new User();
        provider.setId(1L);
        provider.setFirstName("Carlos");
        provider.setLastName("Lopez");
        provider.setRole(User.Role.PROVEEDOR);
        provider.setServiceType("Barbería");
        provider.setActive(true);

        requestDTO = new ScheduleRequestDTO();
        requestDTO.setStartTime(LocalDateTime.now().plusDays(1).withHour(9).withMinute(0));
        requestDTO.setEndTime(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0));
        requestDTO.setAvailableSlots(5);
    }

    // HU-06 Escenario 1: Registro exitoso de franja
    @Test
    void createSchedule_successful() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(provider));
        when(scheduleRepository.findOverlapping(anyLong(), any(), any())).thenReturn(Collections.emptyList());

        Schedule saved = new Schedule();
        saved.setId(10L);
        saved.setProvider(provider);
        saved.setStartTime(requestDTO.getStartTime());
        saved.setEndTime(requestDTO.getEndTime());
        saved.setAvailableSlots(5);
        saved.setActive(true);
        saved.setCreatedAt(LocalDateTime.now());
        when(scheduleRepository.save(any(Schedule.class))).thenReturn(saved);

        ScheduleResponseDTO result = scheduleService.createSchedule(1L, requestDTO);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getAvailableSlots()).isEqualTo(5);
        assertThat(result.isActive()).isTrue();
        verify(scheduleRepository).save(any(Schedule.class));
    }

    // HU-06 Escenario 2: Rango horario inválido (fin <= inicio)
    @Test
    void createSchedule_invalidRange_throws() {
        requestDTO.setEndTime(requestDTO.getStartTime().minusHours(1));
        when(userRepository.findById(1L)).thenReturn(Optional.of(provider));

        assertThatThrownBy(() -> scheduleService.createSchedule(1L, requestDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rango horario no es válido");
    }

    // HU-06 Escenario 3: Franja traslapada
    @Test
    void createSchedule_overlapping_throws() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(provider));
        Schedule existing = new Schedule();
        when(scheduleRepository.findOverlapping(anyLong(), any(), any())).thenReturn(List.of(existing));

        assertThatThrownBy(() -> scheduleService.createSchedule(1L, requestDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("traslape");
    }

    // Rol inválido
    @Test
    void createSchedule_nonProvider_throws() {
        provider.setRole(User.Role.CLIENTE);
        when(userRepository.findById(1L)).thenReturn(Optional.of(provider));

        assertThatThrownBy(() -> scheduleService.createSchedule(1L, requestDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("proveedores");
    }

    // HU-06 Escenario 5: Toggle de estado activo
    @Test
    void toggleScheduleStatus_deactivates() {
        Schedule schedule = new Schedule();
        schedule.setId(10L);
        schedule.setProvider(provider);
        schedule.setActive(true);
        schedule.setStartTime(requestDTO.getStartTime());
        schedule.setEndTime(requestDTO.getEndTime());
        schedule.setAvailableSlots(5);
        schedule.setCreatedAt(LocalDateTime.now());

        when(scheduleRepository.findById(10L)).thenReturn(Optional.of(schedule));
        when(scheduleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ScheduleResponseDTO result = scheduleService.toggleScheduleStatus(10L, 1L);

        assertThat(result.isActive()).isFalse();
    }

    // HU-06 Escenario 5: No puede modificar franja ajena
    @Test
    void toggleScheduleStatus_differentProvider_throws() {
        User otherProvider = new User();
        otherProvider.setId(99L);
        Schedule schedule = new Schedule();
        schedule.setId(10L);
        schedule.setProvider(otherProvider);

        when(scheduleRepository.findById(10L)).thenReturn(Optional.of(schedule));

        assertThatThrownBy(() -> scheduleService.toggleScheduleStatus(10L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("permiso");
    }
}
