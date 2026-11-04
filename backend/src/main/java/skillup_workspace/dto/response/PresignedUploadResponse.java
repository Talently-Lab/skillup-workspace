package skillup_workspace.dto.response;

import java.util.Map;

public record PresignedUploadResponse(
        String uploadUrl,
        String objectKey,
        String contentType,
        int expiresInSeconds,
        Map<String, String> requiredHeaders
) {
}
