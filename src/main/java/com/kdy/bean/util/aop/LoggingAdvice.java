package com.kdy.bean.util.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

//@Aspect
@Component
public class LoggingAdvice {
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	//@Around("within(com.tigen.lms.sched.*) or @annotation(com.tigen.lms.bean.util.annotation.StopWatch)")
	@Around("@annotation(com.kdy.bean.util.annotation.StopWatch)")
	public Object schedStopWatch(ProceedingJoinPoint jointPoint) throws Throwable {
		StopWatch stopWatch = new StopWatch();
		Object proceed = null;
		String type = jointPoint.getSignature().getDeclaringTypeName();
		
		try {
			stopWatch.start();
			
			proceed = jointPoint.proceed();
			
		} finally {
			stopWatch.stop();
			//logger.info(type + "." + jointPoint.getSignature().getName() + " ConsumeTime :: " + TimeUnit.SECONDS.convert(stopWatch.getTotalTimeNanos(), TimeUnit.NANOSECONDS) + "s");
			logger.warn(type + "." + jointPoint.getSignature().getName() + " > "+ stopWatch.getTotalTimeMillis() + "ms");
			//logger.info(" #######getTotalTimeNanos######## " + stopWatch.getTotalTimeNanos() + "s");
			//logger.info(" #######getLastTaskName######## " + stopWatch.getLastTaskName() + "s");
			//logger.info(" #######prettyPrintshortSummary######## " + stopWatch.prettyPrint());
			//logger.warn(type + "." + jointPoint.getSignature().getName() + " > " + stopWatch.shortSummary());
			//logger.info(" #######currentTaskName######## " + stopWatch.currentTaskName());
		}
		return proceed;
	}
}


