package skillup_workspace.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import skillup_workspace.dto.response.AdminUserResponseDTO;
import skillup_workspace.repository.UserRepository;
import skillup_workspace.service.AdminUserService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;

    @Override
    public List<AdminUserResponseDTO> getAllUsers() {
        return userRepository.findAll(Sort.by(
                        Sort.Order.asc("lastName"),
                        Sort.Order.asc("firstName"),
                        Sort.Order.asc("id")
                ))
                .stream()
                .map(user -> new AdminUserResponseDTO(
                        user.getId(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.getEmail(),
                        user.getRole()
                ))
                .toList();
    }
}
