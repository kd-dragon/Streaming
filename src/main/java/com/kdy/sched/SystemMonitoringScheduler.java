package com.kdy.sched;

import java.text.NumberFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SystemMonitoringScheduler {
	private final static Logger log  = LoggerFactory.getLogger(SystemMonitoringScheduler.class);

	@Scheduled(fixedDelay = 60000, initialDelay = 1000)
	public void service() throws Exception {
		Thread.currentThread().setName("TG_"+getClass().getSimpleName());
		
		StringBuilder sb = new StringBuilder();
		Runtime runtime = Runtime.getRuntime();;
		NumberFormat format =  NumberFormat.getInstance();
		
		long maxMemory = runtime.maxMemory();
		long allocatedMemory = runtime.totalMemory();
		long freeMemory = runtime.freeMemory();
		
		sb.append("\nfree memory: " + format.format(freeMemory / 1024) + "\n");
		sb.append("allocated memory: " + format.format(allocatedMemory / 1024) + "\n");
		sb.append("max memory: " + format.format(maxMemory / 1024) + "\n");
		sb.append("total free memory: " + format.format((freeMemory + (maxMemory - allocatedMemory)) / 1024) + "\n");
		
		log.info(sb.toString());
		sb = null;
	}
}
