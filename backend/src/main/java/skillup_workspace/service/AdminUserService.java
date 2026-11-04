package skillup_workspace.service;

import skillup_workspace.dto.response.AdminUserResponseDTO;

import java.util.List;

public interface AdminUserService {

    List<AdminUserResponseDTO> getAllUsers();
}
