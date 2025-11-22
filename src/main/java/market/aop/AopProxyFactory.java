package market.aop;

import market.domain.AuditEvent;
import market.exception.AuditMethodNotFoundException;
import market.service.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.function.Supplier;

public final class AopProxyFactory {

    private AopProxyFactory() {
    }

    /**
     * Создаёт JDK-прокси для сервиса.
     *
     * @param target              реальный объект сервиса
     * @param iface               интерфейс, который он реализует
     * @param audit               сервис аудита
     * @param currentUserSupplier поставщик текущего пользователя
     * @param <T>                 тип интерфейса
     * @return прокси, который оборачивает вызовы методов target
     */
    @SuppressWarnings("unchecked")
    public static <T> T createProxy(
            T target,
            Class<T> iface,
            AuditService audit,
            Supplier<String> currentUserSupplier
    ) {
        Logger log = LoggerFactory.getLogger(iface);

        InvocationHandler handler = (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                return method.invoke(target, args);
            }

            long start = System.nanoTime();
            boolean success = false;
            try {
                Object result = method.invoke(target, args);
                success = true;

                handleAuditIfNeeded(method, target, args, audit, currentUserSupplier);

                return result;
            } catch (InvocationTargetException ite) {
                throw ite.getTargetException();
            } finally {
                long durationMs = (System.nanoTime() - start) / 1_000_000;
                log.info("Method {}.{} took {} ms (success={})",
                        iface.getSimpleName(),
                        method.getName(),
                        durationMs,
                        success);
            }
        };

        return (T) Proxy.newProxyInstance(
                iface.getClassLoader(),
                new Class<?>[]{iface},
                handler
        );
    }

    private static void handleAuditIfNeeded(
            Method interfaceMethod,
            Object target,
            Object[] args,
            AuditService audit,
            Supplier<String> currentUserSupplier
    ) {
        try {
            Method implMethod = target.getClass()
                    .getMethod(interfaceMethod.getName(), interfaceMethod.getParameterTypes());

            Audited ann = implMethod.getAnnotation(Audited.class);
            if (ann == null) {
                return;
            }

            String username = safeCurrentUser(currentUserSupplier);
            String details = buildDetails(ann, interfaceMethod, args);

            AuditEvent event = new AuditEvent(
                    username,
                    ann.value(),
                    details
            );
            event.setTimestamp(LocalDateTime.now());

            audit.append(event);
        } catch (NoSuchMethodException e) {
            throw new AuditMethodNotFoundException(
                    "Audit failed: method '" + interfaceMethod.getName() +
                            "' not found in implementation class: " + target.getClass().getName(),
                    e
            );
        }
    }

    private static String safeCurrentUser(Supplier<String> supplier) {
        try {
            String u = supplier != null ? supplier.get() : null;
            return (u == null || u.isBlank()) ? "-" : u;
        } catch (Exception e) {
            return "-";
        }
    }

    private static String buildDetails(
            Audited ann,
            Method method,
            Object[] args
    ) {
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
