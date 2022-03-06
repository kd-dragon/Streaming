package com.kdy.sched;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.kdy.bean.netty.NettyServerRunnable;
import com.kdy.bean.netty.initialize.NettyRtspStreamingInitializer;
import com.kdy.dto.NettyVO;

@Component
public class RtspStreamingScheduler {
	
	private final static Logger log  = LoggerFactory.getLogger(RtspStreamingScheduler.class);
	
	@Value("${netty.rtsp.port}")
	private int rtspPort;
	
	@Value("${netty.rtsp.useYn}")
	private String rtspUseYn;
	
	@Autowired
	private NettyVO vo;
	
	@Autowired
	private NettyRtspStreamingInitializer initializer;
	
	//@Scheduled(fixedDelay = 10000, initialDelay = 1000)
	public void service() throws Exception {
		Thread.currentThread().setName("TG_"+getClass().getSimpleName());
		
		if(rtspUseYn.equalsIgnoreCase("Y")) {
			if (vo.getThread("rtsp") == null) {
				log.info("NettyRtspStreamingScheduler service() - RTSP Server initialize START");
				NettyServerRunnable runnable = new NettyServerRunnable(vo, initializer, rtspPort, "rtsp");
				runnable.start();
				vo.setThread("rtsp", runnable);
			} else {
				log.info("NettyRtspStreamingScheduler service() - RTSP Server Running ...");
			}
		}
	}
}
