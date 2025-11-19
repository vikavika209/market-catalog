package market.domain;
import java.time.LocalDateTime;

public class AuditEvent {

    private Long id;
    private String productName;
    private String username;
    private AuditAction action;
    private String details;
    private LocalDateTime timestamp;

    public AuditEvent(String username, AuditAction action, String details) {
        this.username = username;
        this.action = action;
        this.details = details;
        this.timestamp = LocalDateTime.now();
    }

    public AuditEvent() {
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

    public Long getId() {
        return id;
    }

    public String getProductName() {
        return productName;
    }

    @Override
    public String toString() {
        return "[%s] user=%s action=%s details=%s"
                .formatted(timestamp, username, action, details);
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public void setAction(AuditAction action) {
        this.action = action;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }
}