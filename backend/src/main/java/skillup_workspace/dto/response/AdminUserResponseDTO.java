package skillup_workspace.dto.response;

public record AdminUserResponseDTO(
        Long id,
        String firstName,
        String lastName,
        String email,
        String role
) {
}
