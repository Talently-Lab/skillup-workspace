package skillup_workspace.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import skillup_workspace.dto.response.AdminDashboardStatsResponseDTO;
import skillup_workspace.repository.CourseRepository;
import skillup_workspace.repository.EnrollmentRepository;
import skillup_workspace.service.impl.AdminDashboardStatsServiceImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDashboardStatsServiceImplTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @InjectMocks
    private AdminDashboardStatsServiceImpl dashboardStatsService;

    @Test
    void getDashboardStatsCountsCoursesAndStudentEnrollmentsAndMarksCompletedCountUnavailable() {
        when(courseRepository.count()).thenReturn(12L);
        when(enrollmentRepository.countByStudentRole()).thenReturn(35L);

        AdminDashboardStatsResponseDTO result = dashboardStatsService.getDashboardStats();

        assertEquals(12L, result.getTotalCourses());
        assertEquals(35L, result.getTotalEnrollments());
        assertNull(result.getCompletedCourses());
        assertFalse(result.isCompletedCoursesAvailable());
        verify(courseRepository).count();
        verify(enrollmentRepository).countByStudentRole();
        verifyNoMoreInteractions(courseRepository, enrollmentRepository);
    }
}
