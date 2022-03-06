package com.kdy.bean.handler.ts.IF;

import io.netty.channel.ChannelHandlerContext;

public interface IVodMpegTsHandlerFactory {
	public byte[] encodeVodChunkFile(String tsFileName, String vodSeq, String vodFileFullPath, ChannelHandlerContext ctx, long start, long end) throws Exception;
}
