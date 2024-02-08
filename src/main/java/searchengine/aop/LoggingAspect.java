package searchengine.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class LoggingAspect {
    @Before("@annotation(Loggable)")
    public void logBefore(JoinPoint joinPoint) {
        log.info("Before execution of: {}", joinPoint.getSignature().getName());
    }

    @After("@annotation(Loggable)")
    public void logAfter(JoinPoint joinPoint) {
        log.info("After execution of: {}", joinPoint.getSignature().getName());
    }

    @AfterReturning(pointcut = "@annotation(Loggable)", returning = "result")
    public void logAfterReturning(JoinPoint joinPoint, Object result) {
        log.info("AfterReturning from: {} with result: {}", joinPoint.getSignature().getName(), result);
    }

    @AfterThrowing(pointcut = "@annotation(Loggable)", throwing = "exception")
    public void logAfterThrowing(JoinPoint joinPoint, Exception exception) {
        log.info("AfterThrowing from: {} with exception: {}", joinPoint.getSignature().getName(), exception.getMessage());
    }

    @Around("@annotation(Loggable)")
    public Object logAround(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {

        long startTime = System.currentTimeMillis();
        Object result = proceedingJoinPoint.proceed();
        long endTime = System.currentTimeMillis();

        log.info("\n\tAround method: '{}' \n\tLead time: {} milliseconds \n\tis finished with result: {} ",
                proceedingJoinPoint.getSignature().getName(), endTime - startTime, result);

        return result;
    }
}
