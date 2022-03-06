package com.kdy.bean.handler.live;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.FileChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.kdy.bean.handler.live.IF.FileHandlerInterface;
import com.kdy.dto.StreamMemoryVO;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

@Component
public class DirectFileHandler implements FileHandlerInterface {
	
	private Logger log = LoggerFactory.getLogger(DirectFileHandler.class);
	
	private final RedisTemplate<String, byte[]> template;
	private final StreamMemoryVO memoryVO;
	
	@Autowired
	public DirectFileHandler(@Qualifier("redisTemplate") RedisTemplate<String, byte[]> template, StreamMemoryVO memoryVO) {
		this.template = template;
		this.memoryVO = memoryVO;
	}
	
	public ByteBuf getByteBuf(String key, ByteBufAllocator alloc) {
		
		File file = null;
		FileInputStream fis = null;
		ByteBuf buf = null;
		
		//String path = memoryVO.getHlsPathLive() + "/" + key;
		String path = "D:/LIVE/upload/" + key;
		
		log.warn(path);
		
		try {
			
			file = new File(path);
			fis = new FileInputStream(file);
			//log.info("Allocate Buffer Size [file-length] ==== " + Long.valueOf(file.length()).intValue());
			int fileLength = Long.valueOf(file.length()).intValue();
			buf = alloc.heapBuffer(fileLength);
			buf.writeBytes(fis.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, file.length()));
			
		} catch (FileNotFoundException e) {
			log.error(e.getMessage());
		} catch (IOException e) {
			log.error(e.getMessage());
		} finally {
			file = null;
			try {
				if(fis != null) {
					fis.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return buf;
	}
}
