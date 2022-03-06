package com.kdy.bean.handler.live;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.kdy.bean.handler.live.IF.FileHandlerInterface;
import com.kdy.bean.redis.RedisLiveCacheBean;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

@Component
public class LocalFileHandler implements FileHandlerInterface {
	
	private Logger log = LoggerFactory.getLogger(LocalFileHandler.class);
	
	private final RedisLiveCacheBean redisLiveCacheBean;
	
	@Autowired
	public LocalFileHandler(RedisLiveCacheBean redisLiveCacheBean) {
		this.redisLiveCacheBean = redisLiveCacheBean;
	}
	
	public ByteBuf getByteBuf(String liveKey, ByteBufAllocator alloc) {
		
		ByteBuf buf = null;
		byte[] rtnBytes = redisLiveCacheBean.getRedisPushData(liveKey, null);
		
		if(rtnBytes != null && rtnBytes.length > 0) {
			buf = alloc.heapBuffer(rtnBytes.length);
			buf.writeBytes(rtnBytes);
		} 
		
		return buf;
	}
}
