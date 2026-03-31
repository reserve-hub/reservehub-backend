package com.eap15.reservehub.repository;

import com.eap15.reservehub.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Spring genera: SELECT * FROM users WHERE email = ?
    Optional<User> findByEmail(String email);

    // Para verificar duplicados sin traer el objeto completo
    boolean existsByEmail(String email);
}