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
public class RedisSentinelFileHandler implements FileHandlerInterface {
	
	private Logger log = LoggerFactory.getLogger(RedisSentinelFileHandler.class);
	
	private final RedisTemplate<String, byte[]> 	template;
	private final RedisTemplate<String, byte[]> 	templateReplica1;
	//private final RedisTemplate<String, byte[]> 	templateReplica2;
	private final StreamMemoryVO 					streamMemoryVO;
	
	@Autowired
	public RedisSentinelFileHandler(  @Qualifier("redisTemplate") 
									  RedisTemplate<String, byte[]> 		template
									, @Qualifier("redisTemplateReplica1") 
									  RedisTemplate<String, byte[]> 		templateReplica1
									//, @Qualifier("redisTemplateReplica2") 
									//  RedisTemplate<String, byte[]> 		templateReplica2
								    , StreamMemoryVO 						streamMemoryVO
    ) {
		this.template 			= template;
		this.templateReplica1 	= templateReplica1;
		//this.templateReplica2 	= templateReplica2;
		this.streamMemoryVO 	= streamMemoryVO;
	}
	
	@Override
	public ByteBuf getByteBuf(String liveKey, ByteBufAllocator alloc) {
		
		ValueOperations<String, byte[]> valueOperations = null;
		ByteBuf buf = null;
		int redisServerNo = streamMemoryVO.getRedisServerNo();
		
		try {
			//log.info("template hashCode 1:{} 2:{}", template.hashCode(), template2.hashCode());  
			
			if(redisServerNo == 0) {
				//log.info("#### redis redisServerNo {}, key {}", redisServerNo, liveKey);
				valueOperations = templateReplica1.opsForValue();
			} else {
				//log.info("#### redis redisServerNo {}, key {}", redisServerNo, liveKey);
				//valueOperations = templateReplica2.opsForValue();
			}
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
				
				if(redisServerNo == 0) {
					streamMemoryVO.setRedisServerNo(1);
					//log.warn("##### [Null-Data] Redis Server Changed [0 -> 1] ####");
				} else {
					streamMemoryVO.setRedisServerNo(0);
					//log.warn("##### [Null-Data] Redis Server Changed [1 -> 0] ####");
				}
			}
			
		} catch(Exception e) {
			//log.error("redis server error: \n{}", e);
			
			if(redisServerNo == 0) {
				streamMemoryVO.setRedisServerNo(1);
				log.warn("##### [Connection Error] Redis Server Changed [0 -> 1] ####");
				
			} else {
				streamMemoryVO.setRedisServerNo(0);
				//log.warn("##### [Connection Error] Redis Server Changed [1 -> 0] ####");
			}
			
			valueOperations = template.opsForValue();
			byte[] rtnBytes = valueOperations.get(liveKey);
			
			if(rtnBytes != null && rtnBytes.length > 0) {
				buf = Unpooled.wrappedBuffer(rtnBytes);
			}
		}
		
		return buf;
	}
	
}
