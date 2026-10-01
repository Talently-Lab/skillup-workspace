package skillup_workspace.service;

import skillup_workspace.dto.request.LoginRequest;
import skillup_workspace.dto.request.RegisterRequest;
import skillup_workspace.entity.User;

public interface AuthService {

    User register(RegisterRequest request);

    User login(LoginRequest request);
}