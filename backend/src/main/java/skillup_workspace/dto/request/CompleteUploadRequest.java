package skillup_workspace.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import skillup_workspace.storage.UploadPurpose;

public record CompleteUploadRequest(
        @NotBlank @Size(max = 120) String objectKey,
        @NotNull UploadPurpose purpose
) {
}
