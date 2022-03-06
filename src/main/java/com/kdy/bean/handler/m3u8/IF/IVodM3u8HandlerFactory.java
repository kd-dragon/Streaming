package com.kdy.bean.handler.m3u8.IF;

public interface IVodM3u8HandlerFactory {
		
	public byte[] createM3u8File(String vodFileSeq, String vodFileFullPath) throws Exception;
	
}
