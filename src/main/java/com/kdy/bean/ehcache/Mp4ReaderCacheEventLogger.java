package com.kdy.bean.ehcache;

import org.ehcache.event.CacheEvent;
import org.ehcache.event.CacheEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class Mp4ReaderCacheEventLogger implements CacheEventListener<Object, Object> {
	
	private final Logger log  = LoggerFactory.getLogger(Mp4ReaderCacheEventLogger.class);

	@Override
	public void onEvent(CacheEvent<? extends Object, ? extends Object> cacheEvent) {
		//log.info("########### cache event logger message. getKey: {} / getOldValue: {} / getNewValue:{}", cacheEvent.getKey(), cacheEvent.getOldValue(), cacheEvent.getNewValue());
		log.info("### Mp4 Reader cache event logger -> getKey: {} / getType: {}",cacheEvent.getKey(), cacheEvent.getType().name());
	}

}
