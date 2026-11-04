package skillup_workspace.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import skillup_workspace.dto.response.AdminUserResponseDTO;
import skillup_workspace.service.AdminUserService;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AdminUserService adminUserService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminUserController(adminUserService)).build();
    }

    @Test
    @DisplayName("GET /api/v1/admin/users - Retorna usuarios sin exponer credenciales")
    void getAllUsers_ShouldReturnSafeUserFields() throws Exception {
        given(adminUserService.getAllUsers()).willReturn(List.of(
                new AdminUserResponseDTO(
                        1L,
                        "Ana",
                        "Pérez",
                        "ana@example.com",
                        "ALUMNO"
                )
        ));

        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].firstName").value("Ana"))
                .andExpect(jsonPath("$[0].lastName").value("Pérez"))
                .andExpect(jsonPath("$[0].email").value("ana@example.com"))
                .andExpect(jsonPath("$[0].role").value("ALUMNO"))
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist());
    }
}
