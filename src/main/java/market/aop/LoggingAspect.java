package market.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Аспект логирования.
 * <p>
 * Перехватывает вызовы методов, помеченных {@link Logged},
 * измеряет время выполнения и пишет информацию в лог.
 */
@Aspect
@Component
public class LoggingAspect {
    @Around("@annotation(market.aop.Logged)")
    public Object logExecution(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.nanoTime();
        boolean success = false;

        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Class<?> targetClass = signature.getDeclaringType();
        String methodName = signature.getName();

        Logger log = LoggerFactory.getLogger(targetClass);

        try {
            Object result = pjp.proceed();
            success = true;
            return result;
        } finally {
            long durationMs = (System.nanoTime() - start) / 1_000_000;
            log.info("Method {}.{} took {} ms (success={})",
                    targetClass.getSimpleName(),
                    methodName,
                    durationMs,
                    success);
        }
    }
}
