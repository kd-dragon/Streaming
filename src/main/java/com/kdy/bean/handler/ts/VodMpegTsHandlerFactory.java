package com.kdy.bean.handler.ts;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.kdy.bean.handler.ts.IF.IVodMpegTsHandlerFactory;
import com.kdy.dto.StreamMemoryVO;

@Component
public class VodMpegTsHandlerFactory {
	
	private final StreamMemoryVO mvo;
	
	@Autowired
	public VodMpegTsHandlerFactory(StreamMemoryVO mvo) {
		this.mvo = mvo;
	}
	
	public IVodMpegTsHandlerFactory getHandler() {
		
		ApplicationContext ctx = mvo.getApplicationContext();
		
		if(mvo.getRedisUseYn().equalsIgnoreCase("Y")) {
			return ctx.getBean("redisMpegTsHandler", IVodMpegTsHandlerFactory.class);
		} else {
			return ctx.getBean("objectMpegTsHandler", IVodMpegTsHandlerFactory.class);
		}
	}

}
