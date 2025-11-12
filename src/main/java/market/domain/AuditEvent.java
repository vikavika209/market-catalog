package market.domain;
import java.time.LocalDateTime;

public class AuditEvent {
    private final String username;
    private final AuditAction action;
    private final String details;
    private final LocalDateTime timestamp;

    public AuditEvent(String username, AuditAction action, String details) {
        this.username = username;
        this.action = action;
        this.details = details;
        this.timestamp = LocalDateTime.now();
    }

    public String getUsername() {
        return username;
    }

    public AuditAction getAction() {
        return action;
    }

    public String getDetails() {
        return details;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "[%s] user=%s action=%s details=%s"
                .formatted(timestamp, username, action, details);
    }
}