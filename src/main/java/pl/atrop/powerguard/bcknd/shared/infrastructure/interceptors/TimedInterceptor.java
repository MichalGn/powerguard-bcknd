package pl.atrop.powerguard.bcknd.shared.infrastructure.interceptors;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Timed
@Interceptor
@Priority(Interceptor.Priority.APPLICATION + 20)
public class TimedInterceptor {

    private static final int MAX_PARAMETER_LENGTH = 80;

    @AroundInvoke
    public Object timeInvocation(InvocationContext ctx) throws Exception {
        long startNanos = System.nanoTime();
        Logger logger = LoggerFactory.getLogger(ctx.getMethod().getDeclaringClass());
        String methodName = ctx.getMethod().getName();

        Timed timed = ctx.getMethod().getAnnotation(Timed.class);
        if (timed == null) {
            timed = ctx.getTarget().getClass().getAnnotation(Timed.class);
        }
        boolean logParameters = timed == null || timed.logParameters();

        if (logParameters) {
            logger.info("{} - START, parameters:{}", methodName, formatParameters(ctx.getParameters()));
        } else {
            logger.info("{} - START", methodName);
        }

        try {
            Object result = ctx.proceed();
            logger.info("{} - END, duration:{} ms", methodName, elapsedMillis(startNanos));
            return result;
        } catch (Exception e) {
            logger.error("{} - ERROR, duration:{} ms, type:{}, message:{}",
                    methodName,
                    elapsedMillis(startNanos),
                    e.getClass().getSimpleName(),
                    e.getMessage());
            throw e;
        }
    }

    private long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }

    private String formatParameters(Object[] parameters) {
        if (parameters == null || parameters.length == 0) {
            return "[]";
        }

        return Arrays.stream(parameters)
                .map(this::formatParameter)
                .collect(Collectors.joining(", ", "[", "]"));
    }

    private String formatParameter(Object parameter) {
        if (parameter == null) {
            return "null";
        }

        String typeName = parameter.getClass().getSimpleName();
        String lowerTypeName = typeName.toLowerCase();
        if (lowerTypeName.contains("login")
                || lowerTypeName.contains("password")
                || lowerTypeName.contains("token")
                || lowerTypeName.contains("credential")
                || lowerTypeName.contains("secret")) {
            return typeName + "[REDACTED]";
        }

        String value = String.valueOf(parameter);
        if (value.length() > MAX_PARAMETER_LENGTH) {
            value = value.substring(0, MAX_PARAMETER_LENGTH) + "...";
        }
        return value;
    }
}
