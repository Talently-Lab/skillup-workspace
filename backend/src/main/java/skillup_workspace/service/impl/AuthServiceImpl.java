package skillup_workspace.service.impl;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import skillup_workspace.dto.request.LoginRequest;
import skillup_workspace.dto.request.RegisterRequest;
import skillup_workspace.entity.User;
import skillup_workspace.repository.UserRepository;
import skillup_workspace.service.AuthService;

import java.util.Optional;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User register(RegisterRequest request) {

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalStateException("El correo electrónico ya se encuentra registrado.");
        }

        User newUser = new User();
        newUser.setFirstName(request.getFirstName());
        newUser.setLastName(request.getLastName());
        newUser.setEmail(request.getEmail());
        newUser.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        newUser.setRole("ALUMNO");

        return userRepository.save(newUser);
    }

    @Override
    public User login(LoginRequest request) {
        Optional<User> userOptional = userRepository.findByEmail(request.getEmail());

        if (userOptional.isEmpty() || !passwordEncoder.matches(request.getPassword(), userOptional.get().getPasswordHash())) {
            throw new SecurityException("Credenciales inválidas");
        }

        return userOptional.get();
    }
}