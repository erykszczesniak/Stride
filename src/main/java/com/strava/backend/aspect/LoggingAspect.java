package com.strava.backend.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    private static final Set<String> SENSITIVE_TYPE_KEYWORDS = Set.of(
            "authentication", "registration", "password", "login", "credential", "token"
    );

    @Pointcut("execution(* com.strava.backend.controller.*.*(..))")
    public void controllerMethods() {
    }

    @Around("controllerMethods()")
    public Object logControllerMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        log.debug("==> {}.{}() with arguments: {}",
                className, methodName, maskSensitiveArgs(joinPoint.getArgs()));

        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;

            log.debug("<== {}.{}() executed in {} ms",
                    className, methodName, duration);

            return result;
        } catch (Exception e) {
            log.error("<== {}.{}() threw exception: {}",
                    className, methodName, e.getMessage());
            throw e;
        }
    }

    private String maskSensitiveArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        String[] masked = new String[args.length];
        for (int i = 0; i < args.length; i++) {
            if (args[i] == null) {
                masked[i] = "null";
            } else {
                String typeName = args[i].getClass().getSimpleName().toLowerCase();
                boolean isSensitive = SENSITIVE_TYPE_KEYWORDS.stream().anyMatch(typeName::contains);
                masked[i] = isSensitive ? "[REDACTED]" : args[i].toString();
            }
        }
        return Arrays.toString(masked);
    }
}
