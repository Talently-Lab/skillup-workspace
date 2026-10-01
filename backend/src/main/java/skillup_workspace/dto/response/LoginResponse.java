package skillup_workspace.dto.response;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String type;
    private Long id;
    private String email;
    private String role;
}