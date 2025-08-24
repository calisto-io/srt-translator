package de.calisto.srttranslator;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class RestControllerAspect {

  private final Logger log = LoggerFactory.getLogger(getClass());

  @Before("execution(public * de.calisto.srttranslator.controller.*Controller.*(..))")
  public void logBeforeRestCall(JoinPoint pjp) {
    log.info(":::::AOP Before REST call:::::" + pjp);
  }
}
