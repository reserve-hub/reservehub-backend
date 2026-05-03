package com.eap15.reservehub.controller;

import com.eap15.reservehub.dto.ProviderCodeResponseDTO;
import com.eap15.reservehub.service.ProviderCodeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api/provider-codes", produces = "application/json")
public class ProviderCodeController {

    private final ProviderCodeService providerCodeService;

    public ProviderCodeController(ProviderCodeService providerCodeService) {
        this.providerCodeService = providerCodeService;
    }

    // HU-09 Escenario 1: Generar código (solo ADMINISTRADOR)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PostMapping
    public ResponseEntity<ProviderCodeResponseDTO> generateCode() {
        return ResponseEntity.ok(providerCodeService.generateCode());
    }

    // HU-09 Escenario 2: Listar todos los códigos (solo ADMINISTRADOR)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @GetMapping
    public ResponseEntity<List<ProviderCodeResponseDTO>> getAllCodes() {
        return ResponseEntity.ok(providerCodeService.getAllCodes());
    }

    // HU-09 Escenario 3: Desactivar código (solo ADMINISTRADOR)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ProviderCodeResponseDTO> deactivateCode(@PathVariable Long id) {
        return ResponseEntity.ok(providerCodeService.deactivateCode(id));
    }
}
