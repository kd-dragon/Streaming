package com.kdy.bean.handler.m3u8;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.kdy.bean.handler.m3u8.IF.IVodM3u8HandlerFactory;
import com.kdy.dto.StreamMemoryVO;

@Component
public class VodM3u8HandlerFactory {
	
	private final StreamMemoryVO mvo;
	
	@Autowired
	public VodM3u8HandlerFactory(StreamMemoryVO mvo) {
		this.mvo = mvo;
	}
	
	public IVodM3u8HandlerFactory getHandler() throws Exception {
		ApplicationContext ctx = mvo.getApplicationContext();
		IVodM3u8HandlerFactory m3u8HandlerFactory = null;
		
		if(mvo.getRedisUseYn().equalsIgnoreCase("Y")) {
			m3u8HandlerFactory = ctx.getBean("redisM3u8Handler", IVodM3u8HandlerFactory.class);
			return m3u8HandlerFactory;
		} else {
			m3u8HandlerFactory = ctx.getBean("objectM3u8Handler", IVodM3u8HandlerFactory.class);
			return m3u8HandlerFactory;
		}
		
	}
}
