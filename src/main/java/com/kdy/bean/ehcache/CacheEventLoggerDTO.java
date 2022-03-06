package com.kdy.bean.ehcache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import com.kdy.bean.redis.RedisSubscriber;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CacheEventLoggerDTO {
	private final Logger logger = LoggerFactory.getLogger(CacheEventLoggerDTO.class);
	
	// 싱글톤 작업
	private static CacheEventLoggerDTO instance;
	private CacheEventLoggerDTO () {
		
	}
	
	// singleton 객체 한번만 생성
	public static synchronized CacheEventLoggerDTO getInstance () {
		if (instance == null) {
			instance = new CacheEventLoggerDTO();
		}
		return instance;
	}
	
	private RedisMessageListenerContainer redisContainer;
	private RedisSubscriber redisSubscriber;
	
}
