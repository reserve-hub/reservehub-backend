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
import com.eap15.reservehub.security.UserDetailsImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserMapper userMapper;
    @Mock private ProviderCodeRepository providerCodeRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtProvider jwtProvider;

    @InjectMocks
    private UserService userService;

    private User user;
    private UserDTO userDTO;
    private ProviderCode providerCode;
    private ProviderRegisterDTO providerDTO;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setFirstName("Juan");
        user.setLastName("Pérez");
        user.setEmail("juan@example.com");
        user.setPassword("encoded_pw");
        user.setPhone("3001234567");
        user.setRole(User.Role.CLIENTE);
        user.setActive(true);

        userDTO = new UserDTO();
        userDTO.setFirstName("Juan");
        userDTO.setLastName("Pérez");
        userDTO.setEmail("juan@example.com");
        userDTO.setPassword("Password1!");
        userDTO.setPhone("3001234567");

        providerCode = new ProviderCode();
        providerCode.setId(10L);
        providerCode.setCode("PROV-TEST-001");
        providerCode.setUsed(false);
        providerCode.setActive(true);

        providerDTO = new ProviderRegisterDTO();
        providerDTO.setFirstName("María");
        providerDTO.setLastName("García");
        providerDTO.setEmail("maria@example.com");
        providerDTO.setPassword("Password1!");
        providerDTO.setPhone("3109876543");
        providerDTO.setProviderCode("PROV-TEST-001");
        providerDTO.setServiceType("Peluquería");
    }

    // ── registerCliente ──────────────────────────────────────────────────────

    @Test
    void registerCliente_success_returnsUserDTO() {
        when(userRepository.existsByEmail("juan@example.com")).thenReturn(false);
        when(userMapper.toEntity(userDTO)).thenReturn(user);
        when(passwordEncoder.encode(any())).thenReturn("encoded_pw");
        when(userRepository.save(any())).thenReturn(user);
        when(userMapper.toDTO(user)).thenReturn(userDTO);

        UserDTO result = userService.registerCliente(userDTO);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("juan@example.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerCliente_duplicateEmail_throwsIllegalArgument() {
        when(userRepository.existsByEmail("juan@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.registerCliente(userDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("correo ya esta registrado");

        verify(userRepository, never()).save(any());
    }

    // ── registerProveedor ────────────────────────────────────────────────────

    @Test
    void registerProveedor_success_marksCodeAsUsed() {
        User providerUser = new User();
        providerUser.setId(2L);
        providerUser.setRole(User.Role.PROVEEDOR);
        UserDTO providerUserDTO = new UserDTO();
        providerUserDTO.setRole(User.Role.PROVEEDOR);

        when(userRepository.existsByEmail("maria@example.com")).thenReturn(false);
        when(providerCodeRepository.findByCode("PROV-TEST-001")).thenReturn(Optional.of(providerCode));
        when(passwordEncoder.encode(any())).thenReturn("encoded_pw");
        when(userRepository.save(any())).thenReturn(providerUser);
        when(userMapper.toDTO(any(User.class))).thenReturn(providerUserDTO);

        UserDTO result = userService.registerProveedor(providerDTO);

        assertThat(result.getRole()).isEqualTo(User.Role.PROVEEDOR);
        assertThat(providerCode.isUsed()).isTrue();
        verify(providerCodeRepository).save(providerCode);
    }

    @Test
    void registerProveedor_duplicateEmail_throwsIllegalArgument() {
        when(userRepository.existsByEmail("maria@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.registerProveedor(providerDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("correo ya esta registrado");

        verify(providerCodeRepository, never()).findByCode(any());
    }

    @Test
    void registerProveedor_codeNotFound_throwsIllegalArgument() {
        when(userRepository.existsByEmail("maria@example.com")).thenReturn(false);
        when(providerCodeRepository.findByCode("PROV-TEST-001")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.registerProveedor(providerDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalido o expirado");
    }

    @Test
    void registerProveedor_codeAlreadyUsed_throwsIllegalArgument() {
        providerCode.setUsed(true);
        when(userRepository.existsByEmail("maria@example.com")).thenReturn(false);
        when(providerCodeRepository.findByCode("PROV-TEST-001")).thenReturn(Optional.of(providerCode));

        assertThatThrownBy(() -> userService.registerProveedor(providerDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalido o expirado");
    }

    @Test
    void registerProveedor_codeInactive_throwsIllegalArgument() {
        providerCode.setActive(false);
        when(userRepository.existsByEmail("maria@example.com")).thenReturn(false);
        when(providerCodeRepository.findByCode("PROV-TEST-001")).thenReturn(Optional.of(providerCode));

        assertThatThrownBy(() -> userService.registerProveedor(providerDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalido o expirado");
    }

    // ── login ────────────────────────────────────────────────────────────────

    @Test
    void login_validCredentials_returnsTokenAndRole() {
        LoginRequestDTO loginRequest = new LoginRequestDTO();
        loginRequest.setEmail("juan@example.com");
        loginRequest.setPassword("Password1!");

        Authentication auth = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(jwtProvider.generateToken(auth)).thenReturn("jwt-token-xyz");
        when(userRepository.findByEmail("juan@example.com")).thenReturn(Optional.of(user));

        LoginResponseDTO result = userService.login(loginRequest);

        assertThat(result.getToken()).isEqualTo("jwt-token-xyz");
        assertThat(result.getRole()).isEqualTo(User.Role.CLIENTE);
        assertThat(result.getEmail()).isEqualTo("juan@example.com");
    }

    @Test
    void login_badCredentials_throwsIllegalArgument() {
        LoginRequestDTO loginRequest = new LoginRequestDTO();
        loginRequest.setEmail("juan@example.com");
        loginRequest.setPassword("wrongpass");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad credentials"));

        assertThatThrownBy(() -> userService.login(loginRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Correo o contrasena incorrectos");
    }

    @Test
    void login_disabledAccount_throwsIllegalArgument() {
        LoginRequestDTO loginRequest = new LoginRequestDTO();
        loginRequest.setEmail("juan@example.com");
        loginRequest.setPassword("Password1!");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new DisabledException("account disabled"));

        assertThatThrownBy(() -> userService.login(loginRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("inactiva");
    }

    @Test
    void login_activeUserAfterAuth_throwsWhenInactive() {
        LoginRequestDTO loginRequest = new LoginRequestDTO();
        loginRequest.setEmail("juan@example.com");
        loginRequest.setPassword("Password1!");

        user.setActive(false);
        Authentication auth = mock(Authentication.class);
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtProvider.generateToken(auth)).thenReturn("token");
        when(userRepository.findByEmail("juan@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.login(loginRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("inactiva");
    }

    // ── getAllUsers ──────────────────────────────────────────────────────────

    @Test
    void getAllUsers_returnsAllMappedDTOs() {
        User second = new User();
        second.setId(2L);
        UserDTO secondDTO = new UserDTO();
        secondDTO.setId(2L);

        when(userRepository.findAll()).thenReturn(List.of(user, second));
        when(userMapper.toDTO(user)).thenReturn(userDTO);
        when(userMapper.toDTO(second)).thenReturn(secondDTO);

        List<UserDTO> result = userService.getAllUsers();

        assertThat(result).hasSize(2);
    }

    // ── getUserById ──────────────────────────────────────────────────────────

    @Test
    void getUserById_existingId_returnsDTO() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toDTO(user)).thenReturn(userDTO);

        UserDTO result = userService.getUserById(1L);

        assertThat(result.getEmail()).isEqualTo("juan@example.com");
    }

    @Test
    void getUserById_nonExistingId_throwsRuntimeException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Usuario no encontrado");
    }

    // ── updateUser ───────────────────────────────────────────────────────────

    @Test
    void updateUser_sameEmail_updatesSuccessfully() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);
        when(userMapper.toDTO(any())).thenReturn(userDTO);

        UserDTO result = userService.updateUser(1L, userDTO);

        assertThat(result).isNotNull();
        verify(userRepository).save(user);
    }

    @Test
    void updateUser_newEmailNotTaken_updatesSuccessfully() {
        userDTO.setEmail("new@example.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.save(any())).thenReturn(user);
        when(userMapper.toDTO(any())).thenReturn(userDTO);

        UserDTO result = userService.updateUser(1L, userDTO);

        assertThat(result).isNotNull();
    }

    @Test
    void updateUser_userNotFound_throwsRuntimeException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser(99L, userDTO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Usuario no encontrado");
    }

    @Test
    void updateUser_newEmailAlreadyTaken_throwsIllegalArgument() {
        userDTO.setEmail("taken@example.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.updateUser(1L, userDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("correo ya esta en uso");
    }

    // ── toggleUserStatus ─────────────────────────────────────────────────────

    @Test
    void toggleUserStatus_activeUser_deactivates() {
        user.setActive(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        UserDTO deactivatedDTO = new UserDTO();
        deactivatedDTO.setActive(false);
        when(userMapper.toDTO(user)).thenReturn(deactivatedDTO);

        UserDTO result = userService.toggleUserStatus(1L);

        assertThat(user.isActive()).isFalse();
        assertThat(result.isActive()).isFalse();
        verify(userRepository).save(user);
    }

    @Test
    void toggleUserStatus_inactiveUser_activates() {
        user.setActive(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        UserDTO activatedDTO = new UserDTO();
        activatedDTO.setActive(true);
        when(userMapper.toDTO(user)).thenReturn(activatedDTO);

        userService.toggleUserStatus(1L);

        assertThat(user.isActive()).isTrue();
    }

    @Test
    void toggleUserStatus_nonExistingId_throwsRuntimeException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.toggleUserStatus(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Usuario no encontrado");
    }
}
