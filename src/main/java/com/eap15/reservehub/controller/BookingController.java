package com.eap15.reservehub.controller;

import com.eap15.reservehub.dto.BookingReportDTO;
import com.eap15.reservehub.dto.BookingRequestDTO;
import com.eap15.reservehub.dto.BookingResponseDTO;
import com.eap15.reservehub.dto.RescheduleRequestDTO;
import com.eap15.reservehub.entity.Booking;
import com.eap15.reservehub.security.UserDetailsImpl;
import com.eap15.reservehub.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping(value = "/api/bookings", produces = "application/json")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    // ── Sprint 2 ──────────────────────────────────────────────────────────────

    // HU-08 Escenario 1: Crear reserva (solo CLIENTE)
    @PreAuthorize("hasRole('CLIENTE')")
    @PostMapping
    public ResponseEntity<BookingResponseDTO> createBooking(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @Valid @RequestBody BookingRequestDTO dto) {
        Long clientId = principal.getUser().getId();
        return ResponseEntity.ok(bookingService.createBooking(clientId, dto));
    }

    // ── Sprint 3 — HU-10: Cancelación y reagendamiento ───────────────────────

    /**
     * HU-10 Escenarios 1-3: Cancelar una reserva propia.
     * PATCH /api/bookings/{id}/cancel
     */
    @PreAuthorize("hasRole('CLIENTE')")
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<BookingResponseDTO> cancelBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        Long clientId = principal.getUser().getId();
        return ResponseEntity.ok(bookingService.cancelBooking(id, clientId));
    }

    /**
     * HU-10 Escenarios 4-6: Reagendar una reserva propia.
     * PATCH /api/bookings/{id}/reschedule
     */
    @PreAuthorize("hasRole('CLIENTE')")
    @PatchMapping("/{id}/reschedule")
    public ResponseEntity<BookingResponseDTO> rescheduleBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl principal,
            @Valid @RequestBody RescheduleRequestDTO dto) {
        Long clientId = principal.getUser().getId();
        return ResponseEntity.ok(bookingService.rescheduleBooking(id, dto, clientId));
    }

    // ── Sprint 3 — HU-11: Historial de reservas ──────────────────────────────

    /**
     * HU-11 Escenarios 1-5: Historial del cliente con filtros opcionales.
     * GET /api/bookings/mine?status=CONFIRMED&from=2026-01-01T00:00:00&to=2026-12-31T23:59:59
     */
    @PreAuthorize("hasRole('CLIENTE')")
    @GetMapping("/mine")
    public ResponseEntity<List<BookingResponseDTO>> getMyBookings(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @RequestParam(required = false) Booking.BookingStatus status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        Long clientId = principal.getUser().getId();
        return ResponseEntity.ok(bookingService.getMyBookingsFiltered(clientId, status, from, to));
    }

    /**
     * HU-11 Escenario 6: El proveedor consulta reservas de su agenda.
     * GET /api/bookings/provider/mine?from=...&to=...
     */
    @PreAuthorize("hasRole('PROVEEDOR')")
    @GetMapping("/provider/mine")
    public ResponseEntity<List<BookingResponseDTO>> getProviderBookings(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        Long providerId = principal.getUser().getId();
        return ResponseEntity.ok(bookingService.getBookingsByProvider(providerId, from, to));
    }

    // ── Sprint 3 — HU-12: Reportes operativos ────────────────────────────────

    /**
     * HU-12 Escenario 1: Reporte general para administrador.
     * GET /api/bookings/report?from=...&to=...
     */
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @GetMapping("/report")
    public ResponseEntity<BookingReportDTO> getAdminReport(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(bookingService.getAdminReport(from, to));
    }

    /**
     * HU-12 Escenarios 2-3: Reporte del proveedor (sus reservas + ocupación).
     * GET /api/bookings/report/mine?from=...&to=...
     */
    @PreAuthorize("hasRole('PROVEEDOR')")
    @GetMapping("/report/mine")
    public ResponseEntity<BookingReportDTO> getProviderReport(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        Long providerId = principal.getUser().getId();
        return ResponseEntity.ok(bookingService.getProviderReport(providerId, from, to));
    }
}
