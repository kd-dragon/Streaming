package com.kdy.bean.netty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kdy.dto.NettyVO;

import io.netty.channel.ChannelHandler;

public class NettyServerRunnable extends Thread {
	
	private final static Logger log = LoggerFactory.getLogger(NettyServerRunnable.class);
	
	NettyVO vo;
	NettyServer nettyServer;
	ChannelHandler handler;
	
	int port;
	String type;
	
	public NettyServerRunnable(NettyVO vo, ChannelHandler handler, int port, String type) {
		this.vo = vo;
		this.handler = handler;
		this.port = port;
		this.type = type;
		this.nettyServer = (NettyServer)  vo.getApplicationContext().getBean(NettyServer.class);
	}
	
	@Override
	public void run() {
		log.info("NettyServerRunnable run()");
		try {
			nettyServer.service(handler, port, type);
			
		} catch(Exception e) {
			for(StackTraceElement st : e.getStackTrace()) {
				if(st.toString().startsWith("com.kdy")) {
					log.error(st.toString());
				}
			}
		}
	}
}
