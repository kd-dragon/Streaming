package com.kdy.bean.handler.m3u8.IF;

public interface ILiveM3u8HandlerFactory {

	public byte[] createLiveM3u8File(String liveFileSeq, String liveFilePath) throws Exception;
}
