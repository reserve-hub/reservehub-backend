package com.eap15.reservehub.service;

import com.eap15.reservehub.dto.ProviderCodeResponseDTO;
import com.eap15.reservehub.entity.ProviderCode;
import com.eap15.reservehub.repository.ProviderCodeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProviderCodeServiceTest {

    @Mock
    private ProviderCodeRepository providerCodeRepository;

    @InjectMocks
    private ProviderCodeService providerCodeService;

    // HU-09 Escenario 1: Generación exitosa de código
    @Test
    void generateCode_createsActiveUnusedCode() {
        when(providerCodeRepository.save(any(ProviderCode.class))).thenAnswer(inv -> {
            ProviderCode pc = inv.getArgument(0);
            pc.setId(1L);
            return pc;
        });

        ProviderCodeResponseDTO result = providerCodeService.generateCode();

        assertThat(result.getCode()).startsWith("PROV-");
        assertThat(result.isUsed()).isFalse();
        assertThat(result.isActive()).isTrue();
        verify(providerCodeRepository).save(any(ProviderCode.class));
    }

    // HU-09 Escenario 2: Listar códigos
    @Test
    void getAllCodes_returnsList() {
        ProviderCode pc1 = new ProviderCode("PROV-AABBCCDD");
        pc1.setId(1L);
        ProviderCode pc2 = new ProviderCode("PROV-EEFFGGHH");
        pc2.setId(2L);
        pc2.setUsed(true);

        when(providerCodeRepository.findAll()).thenReturn(List.of(pc1, pc2));

        List<ProviderCodeResponseDTO> result = providerCodeService.getAllCodes();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCode()).isEqualTo("PROV-AABBCCDD");
        assertThat(result.get(1).isUsed()).isTrue();
    }

    // HU-09 Escenario 3: Desactivar código no utilizado
    @Test
    void deactivateCode_success() {
        ProviderCode pc = new ProviderCode("PROV-XYZXYZ12");
        pc.setId(5L);
        pc.setUsed(false);
        pc.setActive(true);

        when(providerCodeRepository.findById(5L)).thenReturn(Optional.of(pc));
        when(providerCodeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProviderCodeResponseDTO result = providerCodeService.deactivateCode(5L);

        assertThat(result.isActive()).isFalse();
        verify(providerCodeRepository).save(pc);
    }

    // HU-09 Escenario 4: No se puede desactivar código ya consumido
    @Test
    void deactivateCode_alreadyUsed_throws() {
        ProviderCode pc = new ProviderCode("PROV-CONSUMED1");
        pc.setId(6L);
        pc.setUsed(true);
        pc.setActive(true);

        when(providerCodeRepository.findById(6L)).thenReturn(Optional.of(pc));

        assertThatThrownBy(() -> providerCodeService.deactivateCode(6L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("consumido");
    }

    // Código no encontrado
    @Test
    void deactivateCode_notFound_throws() {
        when(providerCodeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> providerCodeService.deactivateCode(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no encontrado");
    }
}
