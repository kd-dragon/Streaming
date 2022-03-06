package com.kdy.bean.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Component;

import com.kdy.dto.StreamPushVO;

@Component
public class RedisLiveCacheBean {
	private final Logger logger = LoggerFactory.getLogger(RedisLiveCacheBean.class);
	
	@Cacheable(value="liveDataCache", key="#liveKey", unless="#liveKey == null")
	public byte[] getRedisPushData(String liveKey, StreamPushVO streamPushVO) {
		if(streamPushVO == null) {
			return null;
		}
		return streamPushVO.getStreamData();
	}
	
	@CachePut(value="liveDataCache", key="#liveKey", unless="#liveKey == null || #streamPushVO == null")
	public byte[] updateRedisPushData(String liveKey, StreamPushVO streamPushVO) {
		if(streamPushVO == null) {
			return null;
		}
		return streamPushVO.getStreamData();
	}
	
	@Cacheable(value="liveTopicCache", key="#seq", unless="#seq == null")
	public ChannelTopic getLiveTopic(String seq) {
		ChannelTopic topic = null;
		if(seq != null && !seq.equals("")) {
			topic = new ChannelTopic(seq);
		}
		return topic;
	}

}
