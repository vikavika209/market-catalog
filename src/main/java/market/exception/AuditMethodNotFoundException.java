package market.exception;

public class AuditMethodNotFoundException extends RuntimeException {
    public AuditMethodNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
