package com.kdy.config;

import java.io.IOException;
import java.nio.file.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;

import com.kdy.dto.NoSignalVO;

@Configuration
public class SystemConfig {
	
	private Logger logger = LoggerFactory.getLogger(SystemConfig.class);
	
	@Value("classpath:/static/no_signal.ts")
	private Resource tsFile;
	
	@Value("classpath:/static/no_signal.m3u8")
	private Resource m3u8File;
	
	@Bean
	@ConditionalOnMissingBean
	public NoSignalVO loadNoSignalImage() {
		NoSignalVO vo = new NoSignalVO();
		vo.setNoSignalTs(getTsFileBytes());
		vo.setNoSignalM3u8(getM3u8FileBytes());
		return vo;
	}
	
	private byte[] getTsFileBytes() {
		
		byte[] tsBytes = null;
		try {
			tsBytes = FileCopyUtils.copyToByteArray(tsFile.getInputStream());
		} catch (IOException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}
		return tsBytes;
	}
	
	private byte[] getM3u8FileBytes() {
		
		byte[] m3u8Bytes = null;
		
		try {
			m3u8Bytes = FileCopyUtils.copyToByteArray(m3u8File.getInputStream());
		} catch (IOException e) {
			logger.error(e.getMessage());	
			e.printStackTrace();
		}
		return m3u8Bytes;
	}
}
