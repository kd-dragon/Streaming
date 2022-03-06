package com.kdy.bean.handler.m3u8;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.kdy.bean.handler.m3u8.IF.ILiveM3u8HandlerFactory;
import com.kdy.bean.handler.m3u8.IF.IVodM3u8HandlerFactory;
import com.kdy.dto.StreamMemoryVO;
import com.kdy.dto.StreamType;

@Component
public class MasterM3u8Handler implements IVodM3u8HandlerFactory, ILiveM3u8HandlerFactory{
	
	private final Logger log  = LoggerFactory.getLogger(MasterM3u8Handler.class);
	
	private final StreamMemoryVO mvo;
	
	@Autowired
	public MasterM3u8Handler(StreamMemoryVO mvo) {
		this.mvo = mvo;
	}
	
	@Override
	public byte[] createM3u8File(String vodFileSeq, String vodFilePath) throws Exception {
		
		StringBuilder m3u8Line = new StringBuilder();
		m3u8Line.append("#EXTM3U\n");
		m3u8Line.append("#EXT-X-VERSION:3\n");
		
		if(mvo.getHlsType().equals("advance")) {
			m3u8Line.append("#EXT-X-STREAM-INF:BANDWIDTH="+mvo.getBandwidth360p()+",RESOLUTION="+ mvo.getResolutionLow() +"\n");
			m3u8Line.append("/vod"+ vodFilePath + vodFileSeq + "_media_"+mvo.getResolutionLow()+".mp4/low.m3u8\n");
		}
		
		m3u8Line.append("#EXT-X-STREAM-INF:BANDWIDTH="+mvo.getBandwidth480p()+",RESOLUTION="+ mvo.getResolutionMid() +"\n");
		m3u8Line.append("/vod"+ vodFilePath + vodFileSeq + "_media_"+mvo.getResolutionMid()+".mp4/mid.m3u8\n");
		m3u8Line.append("#EXT-X-STREAM-INF:BANDWIDTH="+mvo.getBandwidth720p()+",RESOLUTION="+ mvo.getResolutionHigh() +"\n");
		m3u8Line.append("/vod"+ vodFilePath + vodFileSeq + "_media_"+mvo.getResolutionHigh()+".mp4/high.m3u8");
		//log.debug(m3u8Line.toString());
		return m3u8Line.toString().getBytes();
	}

	//적응형 스트리밍 O master.m3u8
	@Override
	public byte[] createLiveM3u8File(String liveFileSeq, String liveFilePath) throws Exception {
		
		StringBuilder LiveM3u8Line = new StringBuilder();
			LiveM3u8Line.append("#EXTM3U\n");
			LiveM3u8Line.append("#EXT-X-VERSION:3\n");
			
			//High
			LiveM3u8Line.append("#EXT-X-STREAM-INF:BANDWIDTH=" + mvo.getBandwidth720p() + ",RESOLUTION=" + mvo.getResolutionHigh() + "\n");
			LiveM3u8Line.append("/live" + liveFilePath + liveFileSeq + "_" + StreamType.high.ordinal() + "/index.m3u8\n");
	
			//Low
			LiveM3u8Line.append("#EXT-X-STREAM-INF:BANDWIDTH=" + mvo.getBandwidth360p() + ",RESOLUTION=" + mvo.getResolutionLow() + "\n");
			LiveM3u8Line.append("/live" + liveFilePath + liveFileSeq + "_" + StreamType.low.ordinal() + "/index.m3u8\n");
			
			//advance : high, low + mid
			if (mvo.getHlsType().equals("advance")) {
				LiveM3u8Line.append("#EXT-X-STREAM-INF:BANDWIDTH=" + mvo.getBandwidth480p() + ",RESOLUTION=" + mvo.getResolutionMid() + "\n");
				LiveM3u8Line.append("/live" + liveFilePath + liveFileSeq + "_" + StreamType.mid.ordinal() + "/index.m3u8\n");
			}
		
			//log.debug(LiveM3u8Line.toString());
		return LiveM3u8Line.toString().getBytes();
		
	}

}
