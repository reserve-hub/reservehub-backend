package com.eap15.reservehub.controller;

import com.eap15.reservehub.dto.ScheduleRequestDTO;
import com.eap15.reservehub.dto.ScheduleResponseDTO;
import com.eap15.reservehub.security.UserDetailsImpl;
import com.eap15.reservehub.service.ScheduleService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping(value = "/api/schedules", produces = "application/json")
public class ScheduleController {

    @Autowired
    private ScheduleService scheduleService;

    // HU-06 Escenario 1: Crear franja (solo PROVEEDOR)
    @PreAuthorize("hasRole('PROVEEDOR')")
    @PostMapping
    public ResponseEntity<ScheduleResponseDTO> createSchedule(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @Valid @RequestBody ScheduleRequestDTO dto) {
        Long providerId = principal.getUser().getId();
        return ResponseEntity.ok(scheduleService.createSchedule(providerId, dto));
    }

    // HU-07 Escenario 1 y 2: Consultar disponibilidad con filtros
    @GetMapping("/available")
    public ResponseEntity<List<ScheduleResponseDTO>> getAvailable(
            @RequestParam(required = false) Long providerId,
            @RequestParam(required = false) String serviceType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(scheduleService.getAvailableSchedules(providerId, serviceType, date));
    }

    // HU-06: Listar mis franjas (solo PROVEEDOR)
    @PreAuthorize("hasRole('PROVEEDOR')")
    @GetMapping("/mine")
    public ResponseEntity<List<ScheduleResponseDTO>> getMySchedules(
            @AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.ok(scheduleService.getMySchedules(principal.getUser().getId()));
    }

    // HU-06 Escenario 5: Activar / desactivar franja
    @PreAuthorize("hasRole('PROVEEDOR')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ScheduleResponseDTO> toggleStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.ok(scheduleService.toggleScheduleStatus(id, principal.getUser().getId()));
    }
}
