package com.pet.auditspringbootstarter.audit;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;

@Aspect
public class AuditAspect {
    private final AuditWriter auditWriter;
    private final CurrentUserProvider currentUserProvider;

    public AuditAspect(AuditWriter auditWriter, CurrentUserProvider currentUserProvider) {
        this.auditWriter = auditWriter;
        this.currentUserProvider = currentUserProvider;
    }


    @AfterReturning("@annotation(audited)")
    public void auditMethod(JoinPoint jp, Audited audited) {
        MethodSignature signature = (MethodSignature) jp.getSignature();
        Method method = signature.getMethod();

        String details = buildDetails(audited, method, jp.getArgs());
        String username = currentUserProvider
                .getCurrentUsername()
                .filter(u -> !u.isBlank())
                .orElse("-");

        auditWriter.write(username, audited.value(), details);
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
