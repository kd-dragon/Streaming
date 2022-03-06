package com.kdy.sched;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.kdy.bean.netty.NettyServerRunnable;
import com.kdy.bean.netty.initialize.NettyHttpStreamingInitializer;
import com.kdy.dto.NettyVO;

@Component
public class HttpStreamingScheduler {
	
	private final static Logger log  = LoggerFactory.getLogger(HttpStreamingScheduler.class);
	
	@Value("${netty.http.port}")
	private int httpPort;
	
	@Value("${netty.http.useYn}")
	private String httpUseYn;
	
	@Autowired
	private NettyVO vo;
	
	@Autowired
	private NettyHttpStreamingInitializer initializer;
	
	@Scheduled(fixedDelay = 10000, initialDelay = 1000)
	public void service() throws Exception {
		Thread.currentThread().setName("TG_"+getClass().getSimpleName());
		
		if(httpUseYn.equalsIgnoreCase("Y")) {
			if (vo.getThread("http") == null) {
				log.info("HttpStreamingScheduler service() - Http Server initialize START");
				NettyServerRunnable runnable = new NettyServerRunnable(vo, initializer, httpPort, "http");
				runnable.start();
				vo.setThread("http", runnable);
			} else {
				log.debug("HttpStreamingScheduler service() - Http Server Running ...");
			}
		}
	}
}
