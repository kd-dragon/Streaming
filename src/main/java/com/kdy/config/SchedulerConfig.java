package com.kdy.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

@Configuration
@EnableScheduling
public class SchedulerConfig implements SchedulingConfigurer {
	
	private final int POOL_SIZE = 5;
	Logger logger = LoggerFactory.getLogger(SchedulerConfig.class);
	
	@Override
	public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
		logger.info("** POOL_SIZE : "+POOL_SIZE);
		ThreadPoolTaskScheduler threadPool = new ThreadPoolTaskScheduler();
		threadPool.setPoolSize(POOL_SIZE);
		threadPool.initialize();
		taskRegistrar.setTaskScheduler(threadPool);
	}
}
