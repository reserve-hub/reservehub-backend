package com.eap15.reservehub.service;

import com.eap15.reservehub.dto.LoginRequestDTO;
import com.eap15.reservehub.dto.LoginResponseDTO;
import com.eap15.reservehub.dto.ProviderRegisterDTO;
import com.eap15.reservehub.dto.UserDTO;
import com.eap15.reservehub.entity.ProviderCode;
import com.eap15.reservehub.entity.User;
import com.eap15.reservehub.mapper.UserMapper;
import com.eap15.reservehub.repository.ProviderCodeRepository;
import com.eap15.reservehub.repository.UserRepository;
import com.eap15.reservehub.security.JwtProvider;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final ProviderCodeRepository providerCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;

    public UserService(UserRepository userRepository,
                       UserMapper userMapper,
                       ProviderCodeRepository providerCodeRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtProvider jwtProvider) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.providerCodeRepository = providerCodeRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtProvider = jwtProvider;
    }

    // HU-01 Escenario 1: Registro de CLIENTE
    public UserDTO registerCliente(UserDTO userDTO) {
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
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Este correo ya esta registrado");
        }

        ProviderCode providerCode = providerCodeRepository
                .findByCode(dto.getProviderCode())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Codigo de proveedor invalido o expirado"));

        if (providerCode.isUsed() || !providerCode.isActive()) {
            throw new IllegalArgumentException("Codigo de proveedor invalido o expirado");
        }

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

        providerCode.setUsed(true);
        providerCodeRepository.save(providerCode);

        return userMapper.toDTO(userRepository.save(user));
    }

    // HU-02: Inicio de sesion
    public LoginResponseDTO login(LoginRequestDTO loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
            );

            String jwtToken = jwtProvider.generateToken(authentication);

            User user = userRepository.findByEmail(loginRequest.getEmail())
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado post-auth"));

            if (!user.isActive()) {
                throw new IllegalArgumentException("Esta cuenta esta inactiva. Contacte al administrador");
            }

            return new LoginResponseDTO(
                    user.getId(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getEmail(),
                    user.getRole(),
                    "Inicio de sesion exitoso",
                    jwtToken
            );

        } catch (BadCredentialsException e) {
            throw new IllegalArgumentException("Correo o contrasena incorrectos");
        } catch (AccountStatusException e) {
            // Catches DisabledException, LockedException, AccountExpiredException, etc.
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
