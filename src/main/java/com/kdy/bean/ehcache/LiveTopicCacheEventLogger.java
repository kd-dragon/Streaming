package com.kdy.bean.ehcache;

import org.ehcache.event.CacheEvent;
import org.ehcache.event.CacheEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Component;

@Component
public class LiveTopicCacheEventLogger implements CacheEventListener<Object, Object> {
	
	private final Logger log  = LoggerFactory.getLogger(LiveTopicCacheEventLogger.class);
	
	@Override
	public void onEvent(CacheEvent<? extends Object, ? extends Object> cacheEvent) {
		//log.info("########### cache event logger message. getKey: {} / getOldValue: {} / getNewValue:{}", cacheEvent.getKey(), cacheEvent.getOldValue(), cacheEvent.getNewValue());
		//log.info("### Live Topic cache event logger -> getKey: {} / getType: {}", cacheEvent.getKey(), cacheEvent.getType().name());
		
		String key = cacheEvent.getKey().toString();
		String type = cacheEvent.getType().name();
		
		//Expried 된 Topic listener 해제
		if(type.equalsIgnoreCase("EXPIRED") || type.equalsIgnoreCase("REMOVED") || type.equalsIgnoreCase("EVICTED")) {
			CacheEventLoggerDTO cdto = CacheEventLoggerDTO.getInstance();
			log.info("### Finish Live Topic Listener Remove -> getKey: {} / getType: {}", cacheEvent.getKey(), cacheEvent.getType().name());
			ChannelTopic topic = new ChannelTopic(key);
			cdto.getRedisContainer().removeMessageListener(cdto.getRedisSubscriber(), topic);
		}
	}

}
