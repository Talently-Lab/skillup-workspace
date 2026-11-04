package skillup_workspace.exception;

public class InvalidUploadException extends IllegalArgumentException {

    public InvalidUploadException(String message) {
        super(message);
    }

    public InvalidUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}
