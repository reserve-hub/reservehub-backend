package com.eap15.reservehub.service;

import com.eap15.reservehub.dto.ProviderCodeResponseDTO;
import com.eap15.reservehub.entity.ProviderCode;
import com.eap15.reservehub.repository.ProviderCodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ProviderCodeService {

    @Autowired
    private ProviderCodeRepository providerCodeRepository;

    // HU-09 Escenario 1: Generar nuevo código
    @Transactional
    public ProviderCodeResponseDTO generateCode() {
        String code = "PROV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        ProviderCode providerCode = new ProviderCode(code);
        providerCodeRepository.save(providerCode);
        return toDTO(providerCode);
    }

    // HU-09 Escenario 2: Listar todos los códigos
    public List<ProviderCodeResponseDTO> getAllCodes() {
        return providerCodeRepository.findAll()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    // HU-09 Escenario 3: Desactivar código no utilizado
    @Transactional
    public ProviderCodeResponseDTO deactivateCode(Long id) {
        ProviderCode code = providerCodeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Código no encontrado con ID: " + id));

        if (code.isUsed()) {
            throw new IllegalArgumentException("No se puede desactivar un código que ya fue consumido");
        }

        code.setActive(false);
        return toDTO(providerCodeRepository.save(code));
    }

    private ProviderCodeResponseDTO toDTO(ProviderCode pc) {
        return new ProviderCodeResponseDTO(pc.getId(), pc.getCode(), pc.isUsed(), pc.isActive());
    }
}
