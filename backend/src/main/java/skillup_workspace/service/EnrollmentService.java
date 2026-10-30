package skillup_workspace.service;

import skillup_workspace.dto.request.EnrollmentRequestDTO;
import skillup_workspace.dto.response.EnrollmentResponseDTO;

public interface EnrollmentService {
    EnrollmentResponseDTO enrollUser(EnrollmentRequestDTO request);
}