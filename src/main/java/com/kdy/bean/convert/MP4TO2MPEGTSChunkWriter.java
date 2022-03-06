package com.kdy.bean.convert;

import java.io.IOException;
import java.io.Serializable;

import org.apache.mina.core.buffer.IoBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kdy.bean.util.io.event.IF.IEvent;
import com.kdy.bean.util.io.ts.MpegTsSegment;
import com.kdy.bean.util.io.ts.MpegTsSegmentEncryptor;
import com.kdy.bean.util.media.AudioData;
import com.kdy.bean.util.media.VideoData;

/**
 * FLV TO Mpeg2TS Chunk Writer
 *
 */
public class MP4TO2MPEGTSChunkWriter implements IMP4TOMPEGTSWriter, Serializable {

	private static Logger log = LoggerFactory.getLogger(MP4TO2MPEGTSChunkWriter.class);
	
	private IoBuffer data;
	
	private boolean init = false;
	
	private MP4TOMPEGTSWriter mp4TotsWriter;
	
	private boolean isEncrypt = false;
	
	private MpegTsSegmentEncryptor encryptor;
	
	public MP4TO2MPEGTSChunkWriter(IoBuffer videoConfig, IoBuffer audioConfig, boolean isEncrypt) {
	
		mp4TotsWriter = new MP4TOMPEGTSWriter(this, videoConfig, audioConfig);
		mp4TotsWriter.setLastPCRTimecode(0);
		
		this.isEncrypt = isEncrypt;
		if(isEncrypt) {
			encryptor = new MpegTsSegmentEncryptor();
		}
	}
	
	private void initTsHeader() {

		if (init) return;
		mp4TotsWriter.addPAT(0);	
		init = true;
	}
	
	/**
	 * next ts packet
	 * @throws IOException 
	 */
	@Override
	public void nextBlock(long ts , byte[] block) {
		byte[] encData;
		if (isEncrypt)
			encData = encryptor.encryptChunk(block, 0, block.length);
		else 
			encData = block;
		data.put(encData);
	}
	
	/**
	 * start write chunk ts
	 * @param os
	 */
	public void startChunkTS(MpegTsSegment segment) {
		this.data = segment.getBuffer();
		if (isEncrypt) {
			encryptor.init(segment.getEncKeyBytes(), segment.getSequence());
		}
		
		initTsHeader();
	}
	
	public void startChunkTS(IoBuffer data) {
		this.data = data;		
		initTsHeader();
	}
	
	/**
	 * end write chunk ts
	 */
	public void endChunkTS() {		
		if (isEncrypt) {
			byte[] encData = encryptor.encryptFinal();
			data.put(encData);
		}
		init = false;
	}
	
	/**
	 * write stream 
	 * @param event
	 * @param ts
	 * @throws IOException
	 */
	public void writeStreamEvent(IEvent event) throws IOException {
		
		if(event == null) return;
		
		if (event instanceof VideoData) {
			mp4TotsWriter.handleVideo((VideoData) event);
		} else if (event instanceof AudioData) {
			mp4TotsWriter.handleAudio((AudioData) event);
		}
	}
}
