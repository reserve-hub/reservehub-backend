package com.eap15.reservehub.service;

import com.eap15.reservehub.dto.ProviderRegisterDTO;
import com.eap15.reservehub.dto.UserDTO;
import com.eap15.reservehub.dto.LoginResponseDTO;
import com.eap15.reservehub.dto.LoginRequestDTO;
import com.eap15.reservehub.entity.ProviderCode;
import com.eap15.reservehub.entity.User;
import com.eap15.reservehub.mapper.UserMapper;
import com.eap15.reservehub.repository.ProviderCodeRepository;
import com.eap15.reservehub.repository.UserRepository;
import com.eap15.reservehub.security.JwtProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ProviderCodeRepository providerCodeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtProvider jwtProvider;

    // HU-01 Escenario 1: Registro de CLIENTE
    public UserDTO registerCliente(UserDTO userDTO) {
        // Validar correo duplicado (HU-01 escenario de error)
        if (userRepository.existsByEmail(userDTO.getEmail())) {
            throw new IllegalArgumentException("Este correo ya esta registrado");
        }

        User user = userMapper.toEntity(userDTO);
        user.setRole(User.Role.CLIENTE);
        user.setActive(true);
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));

        return userMapper.toDTO(userRepository.save(user));
    }

    // HU-01 Escenario 2: Registro de PROVEEDOR con validacion de codigo
    public UserDTO registerProveedor(ProviderRegisterDTO dto) {
        // Validar correo duplicado
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Este correo ya esta registrado");
        }

        // Buscar el codigo en la tabla provider_codes
        // Si no existe -> invalido. Si existe pero ya fue usado -> invalido
        ProviderCode providerCode = providerCodeRepository
                .findByCode(dto.getProviderCode())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Codigo de proveedor invalido o expirado"));

        // Verificar que el codigo no haya sido usado ya ni esté desactivado (HU-01 / HU-09)
        if (providerCode.isUsed() || !providerCode.isActive()) {
            throw new IllegalArgumentException("Codigo de proveedor invalido o expirado");
        }

        // Construir el usuario proveedor
        User user = new User();
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setPhone(dto.getPhone());
        user.setServiceType(dto.getServiceType());
        user.setServiceDescription(dto.getServiceDescription());
        user.setRole(User.Role.PROVEEDOR);
        user.setActive(true);

        // Marcar el codigo como usado para que no pueda reutilizarse
        providerCode.setUsed(true);
        providerCodeRepository.save(providerCode);

        return userMapper.toDTO(userRepository.save(user));
    }

    // HU-02: Inicio de sesion
    public LoginResponseDTO login(LoginRequestDTO loginRequest) {
        // En lugar de comparar a mano, delegamos en Spring Security (que usa BCrypt internamente si lo configuramos)
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
            );

            // Escenario 3: Generamos el JWT si todo fue OK (validó contra BCrypt)
            String jwtToken = jwtProvider.generateToken(authentication);

            User user = userRepository.findByEmail(loginRequest.getEmail())
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado post-auth"));

            // Escenario 4: Cuenta inactiva o bloqueada (HU-02)
            if (!user.isActive()) {
                throw new IllegalArgumentException("Esta cuenta esta inactiva. Contacte al administrador");
            }

            // Escenario 1: Login exitoso
            return new LoginResponseDTO(
                    user.getId(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getEmail(),
                    user.getRole(),
                    "Inicio de sesion exitoso",
                    jwtToken
            );

        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            // El usuario o la contraseña falló
            throw new IllegalArgumentException("Correo o contrasena incorrectos");
        } catch (org.springframework.security.authentication.DisabledException e) {
            throw new IllegalArgumentException("Esta cuenta esta inactiva. Contacte al administrador");
        }
    }

    // HU-03: Obtener todos los usuarios
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::toDTO)
                .toList();
    }

    // HU-03: Obtener usuario por ID
    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));
        return userMapper.toDTO(user);
    }

    // HU-03: Editar perfil
    public UserDTO updateUser(Long id, UserDTO userDTO) {
        User existing = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));

        if (!existing.getEmail().equals(userDTO.getEmail())
                && userRepository.existsByEmail(userDTO.getEmail())) {
            throw new IllegalArgumentException("Este correo ya esta en uso");
        }

        existing.setFirstName(userDTO.getFirstName());
        existing.setLastName(userDTO.getLastName());
        existing.setEmail(userDTO.getEmail());
        existing.setPhone(userDTO.getPhone());

        return userMapper.toDTO(userRepository.save(existing));
    }

    // HU-04: Activar/desactivar cuenta
    public UserDTO toggleUserStatus(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));
        user.setActive(!user.isActive());
        return userMapper.toDTO(userRepository.save(user));
    }
}