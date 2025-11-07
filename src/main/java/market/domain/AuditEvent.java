
package market.domain;
import java.time.LocalDateTime;
public class AuditEvent {
    private final LocalDateTime ts = LocalDateTime.now();
    private final String username;
    private final String action;
    private final String details;
    public AuditEvent(String username, String action, String details) {
        this.username = username; this.action = action; this.details = details;
    }
    public String format() {
        return "%s | %s | %s | %s".formatted(ts, username == null ? "-" : username, action, details);
    }
}
