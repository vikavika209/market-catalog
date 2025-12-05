package com.pet.auditspringbootstarter.audit;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(AuditAspect.class)
public class AuditAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public AuditAspect auditAspect(
            AuditWriter auditWriter,
            CurrentUserProvider currentUserProvider
    ) {
        return new AuditAspect(auditWriter, currentUserProvider);
    }
}
