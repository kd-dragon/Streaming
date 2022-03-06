package com.kdy.bean.handler.live;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import com.kdy.bean.handler.live.IF.FileHandlerInterface;
import com.kdy.dto.StreamMemoryVO;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;

@Component
public class RedisFileHandler implements FileHandlerInterface {
	
	private Logger log = LoggerFactory.getLogger(RedisFileHandler.class);
	
	private final RedisTemplate<String, byte[]> 	template;
	private final RedisTemplate<String, byte[]> 	templateReplica1;
	private final StreamMemoryVO 					streamMemoryVO;
	
	@Autowired
	public RedisFileHandler(  @Qualifier("redisTemplate") 
							  RedisTemplate<String, byte[]> 		template
							, @Qualifier("redisTemplateReplica1") 
							  RedisTemplate<String, byte[]> 		templateReplica1
						    , StreamMemoryVO 						streamMemoryVO
    ) {
		this.template 			= template;
		this.templateReplica1 	= templateReplica1;
		this.streamMemoryVO 	= streamMemoryVO;
	}
	
	@Override
	public ByteBuf getByteBuf(String liveKey, ByteBufAllocator alloc) {
		
		ValueOperations<String, byte[]> valueOperations = null;
		ByteBuf buf = null;
		
		try {
			
			valueOperations = templateReplica1.opsForValue();
			//log.warn("############# redis Key {}", key);
			byte[] rtnBytes = valueOperations.get(liveKey);
			
			if(rtnBytes != null && rtnBytes.length > 0) {
				//log.debug("RedisFileHandler >> getByteBuf() :: byte-length ===== " + rtnBytes.length);
				buf = Unpooled.wrappedBuffer(rtnBytes);
			} else {
				valueOperations = template.opsForValue();
				rtnBytes = valueOperations.get(liveKey);
				
				if(rtnBytes != null && rtnBytes.length > 0) {
					buf = Unpooled.wrappedBuffer(rtnBytes);
				}
			}
			
		} catch(Exception e) {
			//log.error("redis server error: \n{}", e);
			
			valueOperations = template.opsForValue();
			byte[] rtnBytes = valueOperations.get(liveKey);
			
			if(rtnBytes != null && rtnBytes.length > 0) {
				buf = Unpooled.wrappedBuffer(rtnBytes);
			}
		}
		
		return buf;
	}
	
}
