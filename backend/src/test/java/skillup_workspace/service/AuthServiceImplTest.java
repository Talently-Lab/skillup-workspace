package skillup_workspace.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import skillup_workspace.dto.request.LoginRequest;
import skillup_workspace.dto.request.RegisterRequest;
import skillup_workspace.entity.User;
import skillup_workspace.repository.UserRepository;
import skillup_workspace.service.impl.AuthServiceImpl;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private AuthServiceImpl authService;
    private User mockUser;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setFirstName("Juan");
        mockUser.setLastName("Pérez");
        mockUser.setEmail("juan.perez@example.com");
        mockUser.setPasswordHash("encoded_password");
        mockUser.setRole("ALUMNO");

        registerRequest = new RegisterRequest();
        registerRequest.setFirstName("Juan");
        registerRequest.setLastName("Pérez");
        registerRequest.setEmail("juan.perez@example.com");
        registerRequest.setPassword("password123");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("juan.perez@example.com");
        loginRequest.setPassword("password123");
    }


    @Test
    @DisplayName("register - Debería registrar un nuevo usuario exitosamente cuando el email no existe")
    void register_ShouldSaveUser_WhenEmailDoesNotExist() {
        given(userRepository.findByEmail(registerRequest.getEmail())).willReturn(Optional.empty());
        given(passwordEncoder.encode(registerRequest.getPassword())).willReturn("encoded_password");
        given(userRepository.save(any(User.class))).willReturn(mockUser);

        User result = authService.register(registerRequest);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("juan.perez@example.com", result.getEmail());
        assertEquals("ALUMNO", result.getRole());

        verify(userRepository, times(1)).findByEmail(registerRequest.getEmail());
        verify(passwordEncoder, times(1)).encode(registerRequest.getPassword());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("register - Debería lanzar IllegalStateException cuando el email ya está registrado")
    void register_ShouldThrowException_WhenEmailAlreadyExists() {
        given(userRepository.findByEmail(registerRequest.getEmail())).willReturn(Optional.of(mockUser));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> authService.register(registerRequest)
        );

        assertEquals("El correo electrónico ya se encuentra registrado.", exception.getMessage());
        verify(userRepository, times(1)).findByEmail(registerRequest.getEmail());
        verify(userRepository, never()).save(any(User.class));
    }


    @Test
    @DisplayName("login - Debería retornar el usuario cuando las credenciales son válidas")
    void login_ShouldReturnUser_WhenCredentialsAreValid() {
        given(userRepository.findByEmail(loginRequest.getEmail())).willReturn(Optional.of(mockUser));
        given(passwordEncoder.matches(loginRequest.getPassword(), mockUser.getPasswordHash())).willReturn(true);

        User result = authService.login(loginRequest);

        assertNotNull(result);
        assertEquals("juan.perez@example.com", result.getEmail());
        verify(userRepository, times(1)).findByEmail(loginRequest.getEmail());
        verify(passwordEncoder, times(1)).matches(loginRequest.getPassword(), mockUser.getPasswordHash());
    }

    @Test
    @DisplayName("login - Debería lanzar SecurityException cuando el usuario no existe")
    void login_ShouldThrowException_WhenUserNotFound() {
        given(userRepository.findByEmail(loginRequest.getEmail())).willReturn(Optional.empty());

        SecurityException exception = assertThrows(
                SecurityException.class,
                () -> authService.login(loginRequest)
        );

        assertEquals("Credenciales inválidas", exception.getMessage());
        verify(userRepository, times(1)).findByEmail(loginRequest.getEmail());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("login - Debería lanzar SecurityException cuando la contraseña no coincide")
    void login_ShouldThrowException_WhenPasswordIsIncorrect() {
        given(userRepository.findByEmail(loginRequest.getEmail())).willReturn(Optional.of(mockUser));
        given(passwordEncoder.matches(loginRequest.getPassword(), mockUser.getPasswordHash())).willReturn(false);

        SecurityException exception = assertThrows(
                SecurityException.class,
                () -> authService.login(loginRequest)
        );

        assertEquals("Credenciales inválidas", exception.getMessage());
        verify(userRepository, times(1)).findByEmail(loginRequest.getEmail());
        verify(passwordEncoder, times(1)).matches(loginRequest.getPassword(), mockUser.getPasswordHash());
    }
}