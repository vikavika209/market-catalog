package com.pet.auditspringbootstarter.audit;

public interface AuditWriter {
    void write(String username, String action, String details);
}
