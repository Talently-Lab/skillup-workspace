package skillup_workspace.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import skillup_workspace.dto.response.AdminDashboardStatsResponseDTO;
import skillup_workspace.repository.CourseRepository;
import skillup_workspace.repository.EnrollmentRepository;
import skillup_workspace.service.AdminDashboardStatsService;

@Service
@RequiredArgsConstructor
public class AdminDashboardStatsServiceImpl implements AdminDashboardStatsService {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardStatsResponseDTO getDashboardStats() {
        return new AdminDashboardStatsResponseDTO(
                courseRepository.count(),
                enrollmentRepository.countByStudentRole(),
                null,
                false
        );
    }
}
