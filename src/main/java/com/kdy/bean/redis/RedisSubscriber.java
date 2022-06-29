package com.kdy.bean.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kdy.dto.StreamPushVO;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RedisSubscriber implements MessageListener {
	
	private final Logger logger = LoggerFactory.getLogger(RedisSubscriber.class);
	
	@Qualifier("redisPubsubTemplate")
	private RedisTemplate<String, Object> redisTemplate;
	
	private final ObjectMapper objectMapper;
	
	private final RedisLiveCacheBean redisLiveCacheBean;
	
	/**
	 * <메시지 Subscribe>
	 * 1. Deserialize - StreamPushVO
	 * 2. Save In Local Memory 	 * 
	 */
	@Override
	public void onMessage(Message message, byte[] pattern) {
		
		try {
			
			//Deserialize - StreamPushVO
			String publishMessage = (String) redisTemplate.getStringSerializer().deserialize(message.getBody());
			
			StreamPushVO streamPushVO = objectMapper.readValue(publishMessage, StreamPushVO.class);
			
			//logger.info("streamPushVO > PushKey :: {}", streamPushVO.getKey());
			
			if(streamPushVO.getKey().contains(".m3u8")) {
				if(redisLiveCacheBean.getRedisPushData(streamPushVO.getKey(), streamPushVO) != null) {
					redisLiveCacheBean.updateRedisPushData(streamPushVO.getKey(), streamPushVO);
				};
			} else if(streamPushVO.getKey().contains(".ts")) {
				redisLiveCacheBean.getRedisPushData(streamPushVO.getKey(), streamPushVO);
			} else {
				logger.error("######## Unknown Key Requested : {} ########", streamPushVO.getKey());
			}
			
		} catch(Exception e) {
			logger.error("RedisSubscribe Error : " + e);
		}
	}

}
