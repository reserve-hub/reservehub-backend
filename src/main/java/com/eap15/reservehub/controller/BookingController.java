package com.eap15.reservehub.controller;

import com.eap15.reservehub.dto.BookingRequestDTO;
import com.eap15.reservehub.dto.BookingResponseDTO;
import com.eap15.reservehub.security.UserDetailsImpl;
import com.eap15.reservehub.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/bookings", produces = "application/json")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    // HU-08 Escenario 1: Crear reserva (solo CLIENTE)
    @PreAuthorize("hasRole('CLIENTE')")
    @PostMapping
    public ResponseEntity<BookingResponseDTO> createBooking(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @Valid @RequestBody BookingRequestDTO dto) {
        Long clientId = principal.getUser().getId();
        return ResponseEntity.ok(bookingService.createBooking(clientId, dto));
    }

    // Consultar mis reservas
    @PreAuthorize("hasRole('CLIENTE')")
    @GetMapping("/mine")
    public ResponseEntity<List<BookingResponseDTO>> getMyBookings(
            @AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.ok(bookingService.getMyBookings(principal.getUser().getId()));
    }
}
