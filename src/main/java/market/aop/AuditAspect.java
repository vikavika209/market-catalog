package market.aop;

import market.domain.AuditAction;
import market.domain.AuditEvent;
import market.domain.User;
import market.service.AuditService;
import market.service.AuthService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * Аспект аудита.
 * <p>
 * Перехватывает вызовы методов, помеченных {@link Audited},
 * и записывает {@link AuditEvent} через {@link AuditService}.
 */
@Aspect
@Component
 class AuditAspect {
    private final AuditService auditService;
    private final AuthService authService;

    public AuditAspect(AuditService auditService, AuthService authService) {
        this.auditService = auditService;
        this.authService = authService;
    }

    @AfterReturning("@annotation(market.aop.Audited)")
    public void auditMethod(JoinPoint jp) {
        MethodSignature signature = (MethodSignature) jp.getSignature();
        Method method = signature.getMethod();

        Audited audited = method.getAnnotation(Audited.class);
        if (audited == null) {
            return;
        }

        AuditAction action = audited.value();
        String details = buildDetails(audited, method, jp.getArgs());
        String username = resolveCurrentUser();

        AuditEvent event = new AuditEvent(username, action, details);
        event.setTimestamp(LocalDateTime.now());

        auditService.append(event);
    }

    private String resolveCurrentUser() {
        try {
            return authService.current()
                    .map(User::getUsername)
                    .filter(u -> !u.isBlank())
                    .orElse("-");
        } catch (Exception e) {
            return "-";
        }
    }

    private String buildDetails(Audited ann, Method method, Object[] args) {
        StringBuilder sb = new StringBuilder();

        if (!ann.details().isBlank()) {
            sb.append(ann.details()).append(" | ");
        }

        sb.append("method=").append(method.getName());

        if (args != null && args.length > 0) {
            sb.append(", args=[");
            for (int i = 0; i < args.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(args[i]);
            }
            sb.append("]");
        }

        return sb.toString();
    }
}
