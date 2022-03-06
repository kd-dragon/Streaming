package com.kdy.bean.handler.ts;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

import org.apache.mina.core.buffer.IoBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import com.kdy.bean.convert.MP4TO2MPEGTSChunkWriter;
import com.kdy.bean.handler.ts.IF.IVodMpegTsHandlerFactory;
import com.kdy.bean.util.io.Interface.ITag;
import com.kdy.bean.util.io.Interface.ITagReader;
import com.kdy.bean.util.io.mp4.MP4Frame;
import com.kdy.bean.util.io.mp4.MP4ReaderFactory;
import com.kdy.bean.util.io.ts.TransportStreamUtils;
import com.kdy.bean.util.media.AudioData;
import com.kdy.bean.util.media.VideoData;
import com.kdy.dto.StreamMemoryVO;
import com.kdy.dto.codec.AudioCodec;
import com.kdy.dto.codec.VideoCodec;

import io.netty.channel.ChannelHandlerContext;

@Component
public class RedisMpegTsHandler implements IVodMpegTsHandlerFactory {

	private final Logger log  = LoggerFactory.getLogger(RedisMpegTsHandler.class);
	
	private final MP4ReaderFactory mp4ReaderFactory;
	private final StreamMemoryVO streamMemoryVO;
	private final RedisTemplate<String, byte[]> template;
	
	@Autowired
	public RedisMpegTsHandler(MP4ReaderFactory mp4ReaderFactory, StreamMemoryVO streamMemoryVO, @Qualifier("redisTemplate") RedisTemplate<String, byte[]> template) {
		this.mp4ReaderFactory = mp4ReaderFactory;
		this.streamMemoryVO = streamMemoryVO;
		this.template = template;
	}
	
	@Override
	public byte[] encodeVodChunkFile(String tsFileName, String vodFileSeq, String vodFileFullPath, ChannelHandlerContext ctx, long start, long end) throws Exception {
		
		ValueOperations<String, byte[]> valueOperations = template.opsForValue();
		byte[] rtnBytes = valueOperations.get(tsFileName);
		if(rtnBytes != null && rtnBytes.length > 0) {
			return rtnBytes;
		}
		
		//log.info("### MpegTS vodFileFullPath === " + vodFileFullPath + " && tsFileName === " + tsFileName);
		
		IoBuffer buffer = IoBuffer.allocate(4096).setAutoExpand(true);
		byte[] body = null;
		MP4TO2MPEGTSChunkWriter writer;
		ITagReader reader = null;
		File file = new File(vodFileFullPath);
		
		if(file != null && file.exists()) {
			try {
				
				// MP4 Reader 초기 설정 (Cache)
				reader = mp4ReaderFactory.initialize(vodFileSeq, vodFileFullPath, file).copy();
				
				// MP4 Reader File Channel 연결
				reader.setVodFileChannel(file);
				
				// MP4 Sampling Frame List 가져오기 (Cache)
				//reader.setFrames(new LinkedList<MP4Frame>(mp4ReaderFactory.getSamplingList(vodFileSeq, reader)));
				
				// MP4 Streaming Tag 설정
				reader.setStreamingTags();
				
				//Mpeg Ts Writer 초기화 
				writer = initMpegTsWriter(vodFileSeq, start, reader);
				
				/*
				String tsWriterKey = ctx.channel().id().toString() + vodFileSeq;
				if(streamMemoryVO.getChannelMpegTs().get(tsWriterKey) != null){
					writer = streamMemoryVO.getChannelMpegTs().get(tsWriterKey);
				} else {
					streamMemoryVO.getChannelMpegTs().put(tsWriterKey, writer);
				}
				*/
				//reader.position(start - 4);
				
				if(writer == null) {
					log.error("[Initialize Fail] Not readable TAG From Reader!!!");
					return body;
				}
				
				writer.startChunkTS(buffer);
				
				VideoData videoData;
				AudioData audioData;
				
				// TS File 1개 단위 Chunk Convert 처리
				while(reader.hasMoreTags()) {
					if(end != -1 && reader.getBytesRead() + 4 >= end) {
						break;
					}
					ITag tag = reader.readTag();
					
					if(tag == null) { break; }
					if (tag.getDataType() == 0x09) {
						videoData = new VideoData(tag.getBody());
						videoData.setTimestamp(tag.getTimestamp());
						writer.writeStreamEvent(videoData);
					} else if (tag.getDataType() == 0x08) {
						audioData = new AudioData(tag.getBody());
						audioData.setTimestamp(tag.getTimestamp());
						writer.writeStreamEvent(audioData);
					}
				}
				reader.close();
				writer.endChunkTS();
				buffer.flip();
				
				body = new byte[buffer.remaining()];
				buffer.get(body, 0, body.length);
				
			} catch(IOException e) {
				for(StackTraceElement st : e.getStackTrace()) { 
					if(st.toString().startsWith("com.kdy")) {
						log.error(st.toString()); 
					} 
				}
				return body;
			}
			

			try {
				valueOperations.set(tsFileName, body, 30, TimeUnit.MINUTES);
			} catch(Exception e) {
				log.warn("### FAIL to Set TS-file Redis ### \n" + e.getMessage());
			}
		}
		
		return body;
	}
	
	private MP4TO2MPEGTSChunkWriter initMpegTsWriter(String vodSeq, long start, ITagReader reader) throws Exception {
		
		boolean audioChecked = false;
		boolean videoChecked = false;
		IoBuffer videoConfig  = null;
		IoBuffer audioConfig  = null;
		
		if (start > 0) {				
			ITag tag;
			for (int i = 0; i < 10; i++) {
				if (audioChecked && videoChecked) break;
				tag = reader.readTag();
				if (tag == null) return null;
				if (ITag.TYPE_VIDEO == tag.getDataType()) {
					videoChecked = true;
					if (TransportStreamUtils.getVideoCodec(tag.getBody().get(0)) == VideoCodec.AVC.getId() && tag.getBody().get(1) == 0x00) {
					//if (TransportStreamUtils.getVideoCodec(tag.getBody().getByte(0)) == VideoCodec.AVC.getId() && tag.getBody().getByte(1) == 0x00) {
						videoConfig = tag.getBody();
					}
				} else if (ITag.TYPE_AUDIO == tag.getDataType()) {
					audioChecked = true;
					if (TransportStreamUtils.getAudioCodec(tag.getBody().get(0)) == AudioCodec.AAC.getId() && tag.getBody().get(1) == 0x00) {
					//if (TransportStreamUtils.getAudioCodec(tag.getBody().getByte(0)) == AudioCodec.AAC.getId() && tag.getBody().getByte(1) == 0x00) {
						audioConfig = tag.getBody();
					}
				}
			}
			reader.position(start - 4);
		}
		
		return new MP4TO2MPEGTSChunkWriter(videoConfig, audioConfig, false);
	}
}
