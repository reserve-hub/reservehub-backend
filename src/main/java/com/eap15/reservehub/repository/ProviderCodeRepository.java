package com.eap15.reservehub.repository;

import com.eap15.reservehub.entity.ProviderCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProviderCodeRepository extends JpaRepository<ProviderCode, Long> {

    // Busca el codigo por su valor string
    // Spring genera: SELECT * FROM provider_codes WHERE code = ?
    Optional<ProviderCode> findByCode(String code);
}