package com.kdy.dto;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import io.netty.channel.ChannelHandlerContext;
import lombok.Data;

@Data
@Component
public class NettyVO {
	
	private ChannelHandlerContext httpListenerCtx = null;
	private Thread httpListenerThread = null;
	private ChannelHandlerContext rtspListenerCtx = null;
	private Thread rtspListenerThread = null;
	
	@Value("${netty.http.ssl.path.cert}")
	private String httpSslCertPath;
	
	@Value("${netty.http.ssl.path.key}")
	private String httpSslKeyPath;
	
	@Value("${netty.http.ssl.useYn}")
	private String httpSslUseYn;
	
	@Value("${netty.rtsp.ssl.path.cert}")
	private String rtspSslCertPath;
	
	@Value("${netty.rtsp.ssl.path.key}")
	private String rtspSslKeyPath;
	
	@Value("${netty.rtsp.ssl.useYn}")
	private String rtspSslUseYn;
	
	@Value("${netty.http.streaming-mode}")
	private String httpStreamingMode;
	
	@Autowired
	private ApplicationContext applicationContext;
	
	public boolean setThread(String type, Thread thread) {
		
		if (type.equals("http")) {
			httpListenerThread = thread;
		} else if(type.equals("rtsp")) {
			rtspListenerThread = thread;
		}
		return true;
	}
	
	public Thread getThread(String type) {
		
		if (type.equals("http")) {
			return httpListenerThread;
		} else if(type.equals("rtsp")) {
			return rtspListenerThread;
		}
		return null;
	}
	
	public boolean setChannelHandlerContext(String type, ChannelHandlerContext ctx) {
		if (type.equals("http")) {
			httpListenerCtx = ctx;
		} else if(type.equals("rtsp")) {
			rtspListenerCtx = ctx;
		}
		return true;
	}
	
	public ChannelHandlerContext getChannelHandlerContext(String type) {
		if (type.equals("http")) {
			return httpListenerCtx;
		} else if(type.equals("rtsp")) {
			return rtspListenerCtx;
		}
		return null;
	}
}
