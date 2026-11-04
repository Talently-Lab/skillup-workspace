package skillup_workspace.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import skillup_workspace.dto.response.AdminDashboardStatsResponseDTO;
import skillup_workspace.service.AdminDashboardStatsService;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminDashboardStatsControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AdminDashboardStatsService dashboardStatsService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AdminDashboardStatsController(dashboardStatsService))
                .build();
    }

    @Test
    void getDashboardStatsReturnsDatabaseCountsAndUnavailableCompletedCount() throws Exception {
        given(dashboardStatsService.getDashboardStats())
                .willReturn(new AdminDashboardStatsResponseDTO(12L, 35L, null, false));

        mockMvc.perform(get("/api/v1/admin/dashboard-stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCourses").value(12))
                .andExpect(jsonPath("$.totalEnrollments").value(35))
                .andExpect(jsonPath("$.completedCourses").doesNotExist())
                .andExpect(jsonPath("$.completedCoursesAvailable").value(false));
    }
}
