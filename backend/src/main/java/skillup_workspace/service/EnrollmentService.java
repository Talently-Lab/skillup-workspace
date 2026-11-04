package skillup_workspace.service;

import skillup_workspace.dto.request.EnrollmentRequestDTO;
import skillup_workspace.dto.response.EnrollmentResponseDTO;
import skillup_workspace.dto.response.CourseStudentEnrollmentResponseDTO;
import skillup_workspace.dto.response.StudentEnrollmentResponseDTO;

import java.util.List;

public interface EnrollmentService {
    EnrollmentResponseDTO enrollUser(EnrollmentRequestDTO request);
    List<StudentEnrollmentResponseDTO> getCurrentUserEnrollments();
    List<CourseStudentEnrollmentResponseDTO> getCourseEnrollments(Long courseId);
}