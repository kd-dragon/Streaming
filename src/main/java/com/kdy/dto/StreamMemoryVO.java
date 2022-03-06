package com.kdy.dto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
public class StreamMemoryVO {
	
	@Value("${hls.path.vod}")
	private String hlsPathVod;
	
	@Value("${spring.redis.vod.useYn}")
	private String redisUseYn;
	
	@Value("${hls.segment.time}")
	private int hlsSegmentTime;
	
	@Value("${hls.segment.max}")
	private int hlsSegmentMax;
	
	@Value("${hls.encryptYn}")
	private String hlsEncryptYn;
	
	@Value("${hls.bandwidth.360p}")
	private String bandwidth360p;
	
	@Value("${hls.bandwidth.480p}")
	private String bandwidth480p;
	
	@Value("${hls.bandwidth.720p}")
	private String bandwidth720p;
	
	@Value("${hls.resolution.low}")
	private String resolutionLow;
	
	@Value("${hls.resolution.mid}")
	private String resolutionMid;
	
	@Value("${hls.resolution.high}")
	private String resolutionHigh;
	
	@Value("${hls.type}")
	private String hlsType;
	
	@Value("${hls.adaptive}")
	private String hlsAdaptive;
	
	private int redisServerNo=1;
	
	@Autowired
	private ApplicationContext applicationContext;
	
	@Cacheable(value="vodFileCache", key="#vodSeq", unless="#vodSeq == null")
	public VodFileVO getVodFileCache(String vodSeq, VodFileVO vodFileVO) {
		return vodFileVO;
	}
	
	@CachePut(value="vodFileCache", key="#vodSeq", unless="#vodSeq == null")
	public VodFileVO updateVodFileCache(String vodSeq, VodFileVO vodFileVO) {
		return vodFileVO;
	}
}
