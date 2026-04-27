package com.eap15.reservehub.controller;

import com.eap15.reservehub.dto.ProviderCodeResponseDTO;
import com.eap15.reservehub.service.ProviderCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/provider-codes", produces = "application/json")
public class ProviderCodeController {

    @Autowired
    private ProviderCodeService providerCodeService;

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
