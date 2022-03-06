package com.kdy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

//@EnableAdminServer
@SpringBootApplication
//@EnableAspectJAutoProxy
public class DdragonStreamingApplication {

	/**
	 * Configuration Bean Package -> com.kdy.config
	 * Netty Framework Server Management Scheduler -> com.kdy.sched.HttpStreamingScheduler
	 * 
	 * @implNote com.kdy.bean.netty.handler
	 * 1) Live - HTTPLiveStreamingHandler 
	 * 2) VOD - HTTPVodStreamingHandler
	 * 3) RTSP 
	 * 
	 * @param kdy
	 */
	
	public static void main(String[] args) {
		SpringApplication.run(DdragonStreamingApplication.class, args);
	}

}
