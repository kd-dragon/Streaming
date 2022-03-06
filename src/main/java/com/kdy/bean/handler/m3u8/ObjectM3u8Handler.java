package com.kdy.bean.handler.m3u8;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import com.kdy.bean.handler.m3u8.IF.IVodM3u8HandlerFactory;
import com.kdy.bean.handler.ts.ObjectMpegTsHandler;
import com.kdy.bean.util.annotation.StopWatch;
import com.kdy.bean.util.io.Interface.IKeyFrameDataAnalyzer;
import com.kdy.bean.util.io.Interface.IKeyFrameDataAnalyzer.KeyFrameMeta;
import com.kdy.bean.util.io.Interface.ITagReader;
import com.kdy.bean.util.io.mp4.MP4ReaderFactory;
import com.kdy.dto.StreamMemoryVO;

@Component
public class ObjectM3u8Handler implements IVodM3u8HandlerFactory {
	
	private final Logger log  = LoggerFactory.getLogger(ObjectMpegTsHandler.class);
	
	private final MP4ReaderFactory mp4ReaderFactory;
	private final StreamMemoryVO streamMemoryVO;
	
	@Autowired
	public ObjectM3u8Handler(MP4ReaderFactory mp4ReaderFactory, StreamMemoryVO streamMemoryVO) {
		this.mp4ReaderFactory = mp4ReaderFactory;
		this.streamMemoryVO = streamMemoryVO;
	}
	
	@StopWatch
	@Override
	@Cacheable(value="m3u8Cache", key="#vodFileSeq")
	public byte[] createM3u8File(String vodFileSeq, String vodFileFullPath) throws Exception {
		
		byte[] body = null;
		File file = new File(vodFileFullPath);
		//log.info("vodFileFullPath == " + vodFileFullPath);
		
		String vodSeq = vodFileSeq.split("_")[0];
		String vodQuality = vodFileSeq.split("_")[1];
		
		if(file != null && file.exists()) {
			
			ITagReader reader = null;
			try {
				
				//MP4 Reader 초기 설정 (Cache)
				reader = mp4ReaderFactory.initialize(vodFileSeq, vodFileFullPath, file).copy();
				//reader = mp4ReaderFactory.initialize(vodFileSeq, vodFileFullPath).copy();
				
				//MP4 Reader File Channel Connect
				reader.setVodFileChannel(file);
				
				//MP4 File Sampling
				//mp4ReaderFactory.getSamplingList(vodFileSeq, reader);
				
			} catch (IOException e) {
				for(StackTraceElement st : e.getStackTrace()) { 
					if(st.toString().startsWith("com.kdy")) {
						log.error(st.toString()); 
					} 
				}
				return body;
			}
			
			KeyFrameMeta keymeta = ((IKeyFrameDataAnalyzer) reader).analyzeKeyFrames();
			long[] positions = keymeta.positions;
			long[] timestamps = keymeta.timestamps;
			int duration = streamMemoryVO.getHlsSegmentTime() * 1000;
			long nextTime = duration;
			long startPos = positions[0];
			int rest = 0;
			
			StringBuilder m3u8Line = new StringBuilder();
			m3u8Line.append("#EXTM3U\n");
			m3u8Line.append("#EXT-X-TARGETDURATION:").append(streamMemoryVO.getHlsSegmentMax()).append("\n");
			//m3u8Line.append("#EXT-X-ALLOW-CACHE:YES\n");
			m3u8Line.append("#EXT-X-PLAYLIST-TYPE:VOD\n");
			m3u8Line.append("#EXT-X-VERSION:3\n");
			m3u8Line.append("#EXT-X-MEDIA-SEQUENCE:1\n");
			
			int seqNum = 1;
			float fixDuration = 0;
			
			for (int i=0; i<positions.length; i++) {
				
				if(timestamps[i] >= nextTime) {
					fixDuration = timestamps[i] - nextTime;
					fixDuration = (duration + fixDuration) / 1000;
					//log.info("timestamps == " + timestamps[i] + " , nextTime == " + nextTime + " , fixDuration == " + fixDuration);
					rest = 0;
					
					m3u8Line.append("#EXTINF:").append(fixDuration).append(",\n");
					
					if(i != (positions.length - 1)) {
						m3u8Line.append(String.format("segment-%s-%s-%s-%s-%s.ts\n", seqNum, startPos, positions[i], vodSeq, vodQuality));
						seqNum ++;
					} else {
						m3u8Line.append(String.format("segment-%s-%s-%s-%s-%s.ts\n", seqNum, startPos, file.length(), vodSeq, vodQuality));
						seqNum ++;
					}
					
					startPos = positions[i];
					nextTime = timestamps[i] + duration; // next time
					
				} else {
					rest ++ ;
				}
			}
			
			reader.close();
			// last time < duration
			if(rest > 0) {
				float lastOneDuration = (duration - (nextTime - timestamps[timestamps.length - 1])) / 1000;
				m3u8Line.append("#EXTINF:").append(lastOneDuration).append(",\n");
				m3u8Line.append(String.format("segment-%s-%s-%s-%s-%s.ts\n", seqNum, startPos, file.length(), vodSeq, vodQuality));
			}
			m3u8Line.append("#EXT-X-ENDLIST\n");
			body = m3u8Line.toString().getBytes();
			
		} else {
			log.error(" ========= FILE NOT FOUND =========== \n" + vodFileFullPath);
			throw new Exception("VIDEO FILE NOT FOUND !");
		}
		
		return body;
	}

}
